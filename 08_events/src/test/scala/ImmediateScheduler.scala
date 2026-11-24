package dev.coilix.events

import dev.coilix.concurrency.ScheduledTask
import dev.coilix.concurrency.TaskScheduler
import scala.concurrent.duration.FiniteDuration

/** A scheduler that runs a one-time task at once in the scheduling thread, before `schedule` returns. */
final class ImmediateScheduler extends TaskScheduler {

  override def schedule(delay: FiniteDuration)(task: => Unit): ScheduledTask = {
    task
    ImmediateScheduler.Done
  }

  override def scheduleAtFixedRate(initialDelay: FiniteDuration, period: FiniteDuration)(task: => Unit): ScheduledTask =
    throw UnsupportedOperationException("repeated tasks")

  override def scheduleWithFixedDelay(initialDelay: FiniteDuration, delay: FiniteDuration)(task: => Unit): ScheduledTask =
    throw UnsupportedOperationException("repeated tasks")
}

/** The task returned by [[dev.coilix.events.ImmediateScheduler]]. */
object ImmediateScheduler {
  private val Done: ScheduledTask = new ScheduledTask {
    override def cancel(): Unit = ()

    override def isCancelled: Boolean = false

    override def isDone: Boolean = true
  }
}
