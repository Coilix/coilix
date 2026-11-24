package dev.coilix.pekko

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import dev.coilix.concurrency.BlockingExecutor
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.concurrency.TaskExecutor
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.BootstrapSetup
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.setup.ActorSystemSetup
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Owns the `ActorSystem` of the application: creates it on the thread pools of Coilix and stops it when the context closes.
 *
 * The default dispatcher runs on the [[dev.coilix.concurrency.TaskExecutor]], which is the compute pool of cats-effect when the
 * cats-effect module is present, and `pekko.actor.internal-dispatcher` is the same dispatcher, so Pekko starts no compute pool of
 * its own. `pekko.actor.default-blocking-io-dispatcher` runs on the [[dev.coilix.concurrency.BlockingExecutor]] through the
 * [[dev.coilix.pekko.BlockingExecutorConfigurator]]. Pekko logs through SLF4J.
 *
 * The configuration of Pekko is `settings.config` over these defaults and the `reference.conf` files of Pekko; it can change any
 * of the defaults except that Pekko registers no shutdown hook of the JVM and does not end the JVM itself, because the context
 * stops the `ActorSystem` with a coordinated shutdown, waiting at most `settings.shutdownTimeout`. When the `ActorSystem` stops
 * without the context asking, as Pekko does after a fatal error, `fatalErrors` receives an
 * [[dev.coilix.pekko.ActorSystemStoppedException]].
 *
 * @param settings
 *   the name, the shutdown limit and the configuration of Pekko
 * @param compute
 *   runs the default dispatcher
 * @param blocking
 *   runs the blocking dispatcher
 * @param fatalErrors
 *   receives a stop of the `ActorSystem` that the context did not ask for
 */
@Slf4s
final class CoilixActorSystem(
  settings:    PekkoSettings,
  compute:     TaskExecutor,
  blocking:    BlockingExecutor,
  fatalErrors: FatalErrorHandler
) {

  private val closing = AtomicBoolean(false)

  /** The `ActorSystem`, ready to run actors and streams. */
  val system: ActorSystem = ActorSystem(settings.systemName, setup)

  system.registerOnTermination(stopped())

  log.info(s"Pekko ActorSystem '${settings.systemName}' runs on the task executor and the blocking executor of Coilix")

  /** Whether the `ActorSystem` was asked to stop by the context. */
  def isClosing: Boolean = closing.get

  /** Stops the `ActorSystem` with a coordinated shutdown when the context closes. Closing it again does nothing. */
  @PreDestroy
  def close(): Unit =
    if (closing.compareAndSet(false, true)) {
      Try(Await.result(CoordinatedShutdown(system).run(CoilixShutdownReason), settings.shutdownTimeout)).failed.foreach(failure =>
        log.warn(s"Pekko ActorSystem '${settings.systemName}' did not stop within ${settings.shutdownTimeout}", failure)
      )
    }

  private def setup: ActorSystemSetup =
    ActorSystemSetup(
      BootstrapSetup(Some(getClass.getClassLoader), Some(config), Some(sharedContext(compute.executionContext))),
      CoilixExecutorsSetup(blocking)
    )

  private def config: Config =
    CoilixActorSystem.required
      .withFallback(settings.config)
      .withFallback(CoilixActorSystem.defaults)
      .withFallback(ConfigFactory.defaultReference(getClass.getClassLoader))
      .resolve()

  private def sharedContext(context: ExecutionContext): ExecutionContext =
    ExecutionContext.fromExecutor(task => context.execute(task), context.reportFailure)

  private def stopped(): Unit =
    if (!closing.get) fatalErrors.handle(Thread.currentThread, ActorSystemStoppedException(settings.systemName))
}

/** The configuration that Coilix gives every `ActorSystem` it creates. */
object CoilixActorSystem {

  /** Keys that the configuration of the application cannot change: the context alone stops the `ActorSystem` and the JVM. */
  val required: Config = ConfigFactory.parseString(
    """
      |pekko.coordinated-shutdown.run-by-jvm-shutdown-hook = off
      |pekko.coordinated-shutdown.exit-jvm = off
      |pekko.jvm-exit-on-fatal-error = off
      |""".stripMargin
  )

  /** Defaults that the configuration of the application can change. */
  val defaults: Config = ConfigFactory.parseString(
    """
      |pekko.loggers = ["org.apache.pekko.event.slf4j.Slf4jLogger"]
      |pekko.logging-filter = "org.apache.pekko.event.slf4j.Slf4jLoggingFilter"
      |pekko.actor.internal-dispatcher = "pekko.actor.default-dispatcher"
      |pekko.actor.default-blocking-io-dispatcher {
      |  type = "Dispatcher"
      |  executor = "dev.coilix.pekko.BlockingExecutorConfigurator"
      |  throughput = 1
      |}
      |""".stripMargin
  )
}
