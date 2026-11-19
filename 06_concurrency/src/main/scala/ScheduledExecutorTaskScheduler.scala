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
 * them. This is the type of the `taskScheduler` bean of the concurrency module.
 *
 * @param name
 *   the name used in log messages
 * @param service
 *   runs the tasks
 * @param shutdownTimeout
 *   how long closing waits for running tasks
 */
final class ScheduledExecutorTaskScheduler(val name: String, service: ScheduledThreadPoolExecutor, shutdownTimeout: FiniteDuration)
    extends TaskScheduler
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

  private def runnable(task: => Unit): Runnable =
    () => GracefulShutdown.logFailure(name)(task)
}

/** Factories of [[dev.coilix.concurrency.ScheduledExecutorTaskScheduler]]. */
object ScheduledExecutorTaskScheduler {

  /** A scheduler with a pool of daemon platform threads configured by `settings`. */
  def apply(settings: SchedulerSettings): ScheduledExecutorTaskScheduler =
    new ScheduledExecutorTaskScheduler(
      settings.threadNamePrefix + "*",
      ScheduledThreadPoolExecutor(settings.poolSize, NamedThreadFactory(settings.threadNamePrefix)),
      settings.shutdownTimeout
    )
}
