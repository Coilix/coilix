package dev.coilix.concurrency

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
 * The [[dev.coilix.concurrency.BlockingExecutor]] of the concurrency module, the type behind the default `blockingExecutor`
 * bean.
 *
 * With `maxConcurrency` set, a task first waits for a permit on its own thread, so at most that many tasks run at once and the
 * others wait without holding a thread of the compute pool.
 *
 * @param name
 *   the name used in log messages
 * @param service
 *   runs the tasks
 * @param shutdownTimeout
 *   how long closing waits for started tasks
 * @param fatalErrors
 *   receives the fatal errors of tasks
 * @param maxConcurrency
 *   how many tasks may run at once, or `None` for no limit
 */
final class ExecutorServiceBlockingExecutor(
  name:            String,
  service:         ExecutorService,
  shutdownTimeout: FiniteDuration,
  fatalErrors:     FatalErrorHandler = FatalErrorHandler.logOnly,
  maxConcurrency:  Option[Int]       = None
) extends ExecutorServiceExecutor(name, service, shutdownTimeout, fatalErrors)
    with BlockingExecutor {

  private val permits = maxConcurrency.map(limit => Semaphore(limit, true))

  /** How many more tasks may start now, or `None` when the executor has no limit. */
  def availablePermits: Option[Int] = permits.map(_.availablePermits)

  override protected def admitted[T](task: => T): T =
    permits.fold(task)(semaphore => withPermit(semaphore)(task))

  private def withPermit[T](semaphore: Semaphore)(task: => T): T = {
    semaphore.acquire()
    try {
      task
    } finally {
      semaphore.release()
    }
  }
}

/** Factories of [[dev.coilix.concurrency.ExecutorServiceBlockingExecutor]]. */
object ExecutorServiceBlockingExecutor {

  /**
   * An executor configured by `settings`: a new virtual thread for each task, or a pool of at most `settings.platformThreads`
   * daemon platform threads when `settings.virtualThreads` is `false` or the JDK has no virtual threads. Idle platform threads
   * end after a minute.
   *
   * @param fatalErrors
   *   receives the fatal errors of tasks, also when they end a thread
   */
  def apply(
    settings:    BlockingSettings,
    fatalErrors: FatalErrorHandler = FatalErrorHandler.logOnly
  ): ExecutorServiceBlockingExecutor = {
    val uncaught = Some(FatalErrors.uncaughtExceptionHandler(fatalErrors))
    val service  =
      if (settings.virtualThreads && ThreadFactories.virtualThreadsAvailable) threadPerTask(settings, uncaught)
      else platformPool(settings, uncaught)
    new ExecutorServiceBlockingExecutor(
      settings.threadNamePrefix + "*",
      service,
      settings.shutdownTimeout,
      fatalErrors,
      settings.maxConcurrency
    )
  }

  private def threadPerTask(settings: BlockingSettings, uncaught: Option[Thread.UncaughtExceptionHandler]): ExecutorService =
    ThreadPoolExecutor(
      0,
      Int.MaxValue,
      0L,
      TimeUnit.SECONDS,
      SynchronousQueue[Runnable](),
      ThreadFactories.named(settings.threadNamePrefix, virtual = true, uncaught)
    )

  private def platformPool(settings: BlockingSettings, uncaught: Option[Thread.UncaughtExceptionHandler]): ExecutorService = {
    val pool = ThreadPoolExecutor(
      settings.platformThreads,
      settings.platformThreads,
      1L,
      TimeUnit.MINUTES,
      LinkedBlockingQueue[Runnable](),
      NamedThreadFactory(settings.threadNamePrefix, uncaught)
    )
    pool.allowCoreThreadTimeOut(true)
    pool
  }
}
