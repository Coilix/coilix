package dev.coilix.concurrency

/** A task registered in a [[dev.coilix.concurrency.TaskScheduler]], which can be cancelled. */
trait ScheduledTask {

  /** Stops further runs of the task. A run that has already started finishes. */
  def cancel(): Unit

  /** Whether the task was cancelled. */
  def isCancelled: Boolean

  /** Whether the task will not run again: a one-time task that has run, or a cancelled task. */
  def isDone: Boolean
}
