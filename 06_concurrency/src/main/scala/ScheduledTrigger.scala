package dev.coilix.concurrency

import scala.concurrent.duration.FiniteDuration

/** When a [[dev.coilix.annotation.Scheduled]] method runs. */
enum ScheduledTrigger {

  /** Every `period` between the starts of runs, beginning after `initialDelay`. */
  case FixedRate(initialDelay: FiniteDuration, period: FiniteDuration)

  /** With `delay` between the end of one run and the start of the next, beginning after `initialDelay`. */
  case FixedDelay(initialDelay: FiniteDuration, delay: FiniteDuration)

  /** A description for log messages, such as `with a fixed delay of 1 minute after 10 seconds`. */
  override def toString: String =
    this match {
      case FixedRate(initialDelay, period) => s"at a fixed rate of $period after $initialDelay"
      case FixedDelay(initialDelay, delay) => s"with a fixed delay of $delay after $initialDelay"
    }
}
