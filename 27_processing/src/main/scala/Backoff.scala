package dev.coilix.processing

import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Growing delays between attempts: `initial * multiplier^(attempt - 1)`, at most `max`, spread by `jitter`.
 *
 * @param initial
 *   the delay before the second attempt
 * @param max
 *   the longest delay
 * @param multiplier
 *   how much each delay grows, at least 1
 * @param jitter
 *   the share, from 0 to 1, by which a delay is randomly shortened or lengthened, so replicas do not retry together
 */
final case class Backoff(
  initial:    FiniteDuration = 1.second,
  max:        FiniteDuration = 2.minutes,
  multiplier: Double         = 2.0,
  jitter:     Double         = 0.2
) {
  require(initial >= Duration.Zero, s"initial must not be negative, got $initial")
  require(max >= initial, s"max $max must not be less than initial $initial")
  require(multiplier >= 1.0, s"multiplier must be at least 1, got $multiplier")
  require(jitter >= 0.0 && jitter <= 1.0, s"jitter must be between 0 and 1, got $jitter")

  /**
   * The delay after the given attempt, counted from 1.
   *
   * @param random
   *   a number from 0 to 1 that places the delay within the jitter, 0.5 for no change
   */
  def delay(attempt: Int, random: Double): FiniteDuration = {
    val base   = math.min(initial.toNanos * math.pow(multiplier, (math.max(attempt, 1) - 1).toDouble), max.toNanos.toDouble)
    val spread = base * (1.0 + jitter * (2.0 * random - 1.0))
    Duration.fromNanos(math.min(spread, max.toNanos.toDouble).toLong)
  }
}

/** Common backoffs. */
object Backoff {

  /** The same delay before every attempt. */
  def fixed(delay: FiniteDuration): Backoff = Backoff(delay, delay, 1.0, 0.0)

  /** Delays that double from `initial` up to `max`, without jitter. */
  def exponential(initial: FiniteDuration, max: FiniteDuration): Backoff = Backoff(initial, max, 2.0, 0.0)
}
