package dev.coilix.concurrency

import scala.concurrent.duration.FiniteDuration

/**
 * Runs tasks after a delay or periodically, on threads managed by Coilix.
 *
 * The concurrency module registers a `taskScheduler` bean, closed with the context. A failure of a periodic task is logged
 * and the next runs still happen:
 *
 * ```scala
 * @Component
 * final class CacheRefresher(scheduler: TaskScheduler, cache: PriceCache) {
 *   private val refresh = scheduler.scheduleWithFixedDelay(0.seconds, 1.minute)(cache.reload())
 * }
 * ```
 */
trait TaskScheduler {

  /** Runs `task` once after `delay`. */
  def schedule(delay: FiniteDuration)(task: => Unit): ScheduledTask

  /**
   * Runs `task` every `period`, measured between the starts of runs, beginning after `initialDelay`. A run that takes longer
   * than `period` delays the next one, so runs never overlap.
   */
  def scheduleAtFixedRate(initialDelay: FiniteDuration, period: FiniteDuration)(task: => Unit): ScheduledTask

  /** Runs `task` repeatedly with `delay` between the end of one run and the start of the next, beginning after `initialDelay`. */
  def scheduleWithFixedDelay(initialDelay: FiniteDuration, delay: FiniteDuration)(task: => Unit): ScheduledTask
}
