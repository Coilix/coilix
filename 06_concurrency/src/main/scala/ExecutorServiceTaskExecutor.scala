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
 * [[dev.coilix.concurrency.GracefulShutdown.close]]. A failure of a task started with `execute` is logged, and a fatal error
 * goes to `fatalErrors`, also when it ends a thread through `submit`. This is the type of the `taskExecutor` bean of the
 * concurrency module.
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
final class ExecutorServiceTaskExecutor(
  val name:        String,
  service:         ExecutorService,
  shutdownTimeout: FiniteDuration,
  fatalErrors:     FatalErrorHandler = FatalErrorHandler.logOnly
) extends TaskExecutor
    with AutoCloseable {

  override val executionContext: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(service, failure => log.error(s"Task on $name failed", failure))

  override def execute(task: => Unit): Unit =
    service.execute(() => FatalErrors.guarded(name, fatalErrors)(task))

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
   *
   * @param fatalErrors
   *   receives the fatal errors of tasks, also when they end a thread
   */
  def apply(settings: ExecutorSettings, fatalErrors: FatalErrorHandler = FatalErrorHandler.logOnly): ExecutorServiceTaskExecutor =
    if (settings.virtualThreads) threadPerTask(settings, fatalErrors) else fixedPool(settings, fatalErrors)

  private def threadPerTask(settings: ExecutorSettings, fatalErrors: FatalErrorHandler): ExecutorServiceTaskExecutor = {
    val factory = ThreadFactories.named(settings.threadNamePrefix, virtual = true, uncaught(fatalErrors))
    val service = ThreadPoolExecutor(0, Int.MaxValue, 0L, TimeUnit.SECONDS, SynchronousQueue[Runnable](), factory)
    new ExecutorServiceTaskExecutor(settings.threadNamePrefix + "*", service, settings.shutdownTimeout, fatalErrors)
  }

  private def fixedPool(settings: ExecutorSettings, fatalErrors: FatalErrorHandler): ExecutorServiceTaskExecutor = {
    val factory = ThreadFactories.named(settings.threadNamePrefix, virtual = false, uncaught(fatalErrors))
    val service = Executors.newFixedThreadPool(settings.poolSize, factory)
    new ExecutorServiceTaskExecutor(settings.threadNamePrefix + "*", service, settings.shutdownTimeout, fatalErrors)
  }

  private def uncaught(fatalErrors: FatalErrorHandler): Option[Thread.UncaughtExceptionHandler] =
    Some(FatalErrors.uncaughtExceptionHandler(fatalErrors))
}
