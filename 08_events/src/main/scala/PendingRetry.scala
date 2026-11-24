package dev.coilix.events

import dev.coilix.concurrency.ScheduledTask

/**
 * A retry of an asynchronous delivery that waits for its delay.
 *
 * The retry is registered before it is scheduled, so a scheduler that runs it at once still finds it, and `task` is filled in
 * afterwards to let closing cancel it.
 *
 * @param listener
 *   the name of the listener to call again
 * @param task
 *   the scheduled run, once it is known
 */
private[events] final case class PendingRetry(listener: String, task: Option[ScheduledTask]) {

  /** This retry with the scheduled run that performs it. */
  def scheduled(run: ScheduledTask): PendingRetry = copy(task = Some(run))
}
