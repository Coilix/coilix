package dev.coilix.concurrency

import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * A [[dev.coilix.concurrency.TaskExecutor]] backed by a JDK `ExecutorService`, which it shuts down when the context closes.
 *
 * Closing stops accepting tasks, waits up to `shutdownTimeout` for the started ones and then interrupts them, as described in
 * [[dev.coilix.concurrency.GracefulShutdown.close]]. This is the type of the `taskExecutor` bean of the concurrency module.
 *
 * @param name
 *   the name used in log messages
 * @param service
 *   runs the tasks
 * @param shutdownTimeout
 *   how long closing waits for started tasks
 */
@Slf4s
final class ExecutorServiceTaskExecutor(val name: String, service: ExecutorService, shutdownTimeout: FiniteDuration)
    extends TaskExecutor
    with AutoCloseable {

  override val executionContext: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(service, failure => log.error(s"Task on $name failed", failure))

  override def execute(task: => Unit): Unit =
    service.execute(() => GracefulShutdown.logFailure(name)(task))

  override def submit[T](task: => T): Future[T] =
    Future(task)(using executionContext)

  /** Whether the executor no longer accepts tasks. */
  def isClosed: Boolean = service.isShutdown

  /** Shuts the executor down when the context closes. Closing it again does nothing. */
  @PreDestroy
  override def close(): Unit = GracefulShutdown.close(name, service, shutdownTimeout)
}

/** Factories of [[dev.coilix.concurrency.ExecutorServiceTaskExecutor]]. */
object ExecutorServiceTaskExecutor {

  /**
   * An executor configured by `settings`: a new virtual thread for each task, or a fixed pool of platform threads when
   * `settings.virtualThreads` is `false`. On a JDK without virtual threads each task gets a new daemon platform thread instead.
   */
  def apply(settings: ExecutorSettings): ExecutorServiceTaskExecutor =
    if (settings.virtualThreads) threadPerTask(settings) else fixedPool(settings)

  private def threadPerTask(settings: ExecutorSettings): ExecutorServiceTaskExecutor = {
    val factory = ThreadFactories.named(settings.threadNamePrefix, virtual = true)
    val service = ThreadPoolExecutor(0, Int.MaxValue, 0L, TimeUnit.SECONDS, SynchronousQueue[Runnable](), factory)
    new ExecutorServiceTaskExecutor(settings.threadNamePrefix + "*", service, settings.shutdownTimeout)
  }

  private def fixedPool(settings: ExecutorSettings): ExecutorServiceTaskExecutor = {
    val factory = ThreadFactories.named(settings.threadNamePrefix, virtual = false)
    val service = Executors.newFixedThreadPool(settings.poolSize, factory)
    new ExecutorServiceTaskExecutor(settings.threadNamePrefix + "*", service, settings.shutdownTimeout)
  }
}
