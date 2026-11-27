package dev.coilix.concurrency

import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration.FiniteDuration

/**
 * The [[dev.coilix.concurrency.TaskExecutor]] of the concurrency module, the type behind the default `taskExecutor` bean.
 *
 * Its [[dev.coilix.concurrency.ExecutorServiceTaskExecutor.apply]] creates a `ForkJoinPool` in asynchronous mode, whose
 * workers steal tasks from each other and keep a task submitted by a worker on the same worker when it is free. That keeps
 * hand-offs between tasks, futures and event listeners on one thread and its processor cache.
 *
 * @param name
 *   the name used in log messages
 * @param service
 *   runs the tasks
 * @param shutdownTimeout
 *   how long closing waits for started tasks
 * @param fatalErrors
 *   receives the fatal errors of tasks
 * @param propagators
 *   carry thread-bound context from the submitting thread to the task
 */
final class ExecutorServiceTaskExecutor(
  name:            String,
  service:         ExecutorService,
  shutdownTimeout: FiniteDuration,
  fatalErrors:     FatalErrorHandler       = FatalErrorHandler.logOnly,
  propagators:     List[ContextPropagator] = Nil
) extends ExecutorServiceExecutor(name, service, shutdownTimeout, fatalErrors, propagators)
    with TaskExecutor

/** Factories of [[dev.coilix.concurrency.ExecutorServiceTaskExecutor]]. */
object ExecutorServiceTaskExecutor {

  /**
   * An executor on a `ForkJoinPool` with `settings.threads` daemon platform threads named `<prefix><number>`.
   *
   * @param fatalErrors
   *   receives the fatal errors of tasks, also when they end a worker thread
   * @param propagators
   *   carry thread-bound context from the submitting thread to the task
   */
  def apply(
    settings:    ComputeSettings,
    fatalErrors: FatalErrorHandler       = FatalErrorHandler.logOnly,
    propagators: List[ContextPropagator] = Nil
  ): ExecutorServiceTaskExecutor = {
    val pool = ForkJoinPool(
      settings.threads,
      workerFactory(settings.threadNamePrefix),
      FatalErrors.uncaughtExceptionHandler(fatalErrors),
      true
    )
    new ExecutorServiceTaskExecutor(settings.threadNamePrefix + "*", pool, settings.shutdownTimeout, fatalErrors, propagators)
  }

  /** Names the workers; the counter is an atomic, because the pool creates its threads outside the runtime of cats-effect. */
  @SuppressWarnings(Array("dev.coilix.warts.ThreadPrimitives"))
  private def workerFactory(prefix: String): ForkJoinPool.ForkJoinWorkerThreadFactory = {
    val counter = AtomicLong()
    pool => named(ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool), s"$prefix${counter.incrementAndGet()}")
  }

  private def named(worker: ForkJoinWorkerThread, name: String): ForkJoinWorkerThread = {
    worker.setName(name)
    worker
  }
}
