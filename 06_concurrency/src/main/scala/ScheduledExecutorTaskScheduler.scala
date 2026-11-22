package dev.coilix.concurrency

import dev.coilix.annotation.PreDestroy
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
 * A [[dev.coilix.concurrency.TaskScheduler]] backed by a JDK `ScheduledThreadPoolExecutor`, which it shuts down when the
 * context closes.
 *
 * Closing drops the tasks that wait for their time, waits up to `shutdownTimeout` for the running ones and then interrupts
 * them. The context of `propagators`, such as the MDC, is captured when a task is scheduled and restored around each run. A
 * failure of a task is logged and a fatal error goes to `fatalErrors`. A periodic task goes on after a logged failure.
 * This is the type of the `taskScheduler` bean of the concurrency module.
 *
 * @param name
 *   the name used in log messages
 * @param service
 *   runs the tasks
 * @param shutdownTimeout
 *   how long closing waits for running tasks
 * @param fatalErrors
 *   receives the fatal errors of tasks
 * @param propagators
 *   carry thread-bound context from the scheduling thread to every run of the task
 */
final class ScheduledExecutorTaskScheduler(
  val name:        String,
  service:         ScheduledThreadPoolExecutor,
  shutdownTimeout: FiniteDuration,
  fatalErrors:     FatalErrorHandler       = FatalErrorHandler.logOnly,
  propagators:     List[ContextPropagator] = Nil
) extends TaskScheduler
    with AutoCloseable {

  service.setRemoveOnCancelPolicy(true)
  service.setExecuteExistingDelayedTasksAfterShutdownPolicy(false)
  service.setContinueExistingPeriodicTasksAfterShutdownPolicy(false)

  override def schedule(delay: FiniteDuration)(task: => Unit): ScheduledTask =
    FutureScheduledTask(service.schedule(runnable(task), delay.toNanos, TimeUnit.NANOSECONDS))

  override def scheduleAtFixedRate(initialDelay: FiniteDuration, period: FiniteDuration)(task: => Unit): ScheduledTask =
    FutureScheduledTask(service.scheduleAtFixedRate(runnable(task), initialDelay.toNanos, period.toNanos, TimeUnit.NANOSECONDS))

  override def scheduleWithFixedDelay(initialDelay: FiniteDuration, delay: FiniteDuration)(task: => Unit): ScheduledTask =
    FutureScheduledTask(service.scheduleWithFixedDelay(runnable(task), initialDelay.toNanos, delay.toNanos, TimeUnit.NANOSECONDS))

  /** Whether the scheduler no longer accepts tasks. */
  def isClosed: Boolean = service.isShutdown

  /** Shuts the scheduler down when the context closes. Closing it again does nothing. */
  @PreDestroy
  override def close(): Unit = GracefulShutdown.close(name, service, shutdownTimeout)

  private def runnable(task: => Unit): Runnable = {
    val snapshot = ContextPropagator.captureAll(propagators)
    () => FatalErrors.guarded(name, fatalErrors)(snapshot.runWith(task))
  }
}

/** Factories of [[dev.coilix.concurrency.ScheduledExecutorTaskScheduler]]. */
object ScheduledExecutorTaskScheduler {

  /**
   * A scheduler with a pool of daemon platform threads configured by `settings`.
   *
   * @param fatalErrors
   *   receives the fatal errors of tasks
   * @param propagators
   *   carry thread-bound context from the scheduling thread to every run of the task
   */
  def apply(
    settings:    SchedulerSettings,
    fatalErrors: FatalErrorHandler       = FatalErrorHandler.logOnly,
    propagators: List[ContextPropagator] = Nil
  ): ScheduledExecutorTaskScheduler =
    new ScheduledExecutorTaskScheduler(
      settings.threadNamePrefix + "*",
      ScheduledThreadPoolExecutor(
        settings.poolSize,
        NamedThreadFactory(settings.threadNamePrefix, Some(FatalErrors.uncaughtExceptionHandler(fatalErrors)))
      ),
      settings.shutdownTimeout,
      fatalErrors,
      propagators
    )
}
