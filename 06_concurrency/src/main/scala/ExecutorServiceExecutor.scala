package dev.coilix.concurrency

import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * A [[dev.coilix.concurrency.ManagedExecutor]] backed by a JDK `ExecutorService`, which it shuts down when the context closes.
 *
 * A failure of a task started with `execute` is logged and a fatal error goes to `fatalErrors`. Closing stops accepting tasks,
 * waits up to `shutdownTimeout` for the started ones and then interrupts them, as described in
 * [[dev.coilix.concurrency.GracefulShutdown.close]].
 *
 * @param name
 *   the name used in log messages
 * @param service
 *   runs the tasks
 * @param shutdownTimeout
 *   how long closing waits for started tasks
 * @param fatalErrors
 *   receives the fatal errors of tasks
 */
@Slf4s
abstract class ExecutorServiceExecutor(
  val name:        String,
  service:         ExecutorService,
  shutdownTimeout: FiniteDuration,
  fatalErrors:     FatalErrorHandler
) extends ManagedExecutor
    with AutoCloseable {

  override val executionContext: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(service, failure => log.error(s"Task on $name failed", failure))

  override def execute(task: => Unit): Unit =
    service.execute(() => FatalErrors.guarded(name, fatalErrors)(admitted(task)))

  override def submit[T](task: => T): Future[T] =
    Future(admitted(task))(using executionContext)

  /** Whether the executor no longer accepts tasks. */
  def isClosed: Boolean = service.isShutdown

  /** Shuts the executor down when the context closes. Closing it again does nothing. */
  @PreDestroy
  override def close(): Unit = GracefulShutdown.close(name, service, shutdownTimeout)

  /** Runs `task` on the thread of the executor; an executor that limits running tasks waits for its turn here. */
  protected def admitted[T](task: => T): T = task
}
