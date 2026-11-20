package dev.coilix.concurrency

import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * How many times failed work is tried and how long to wait between the tries.
 *
 * The wait grows exponentially from `initialDelay` by `multiplier` after each failed try and never exceeds `maxDelay`. Each
 * wait is spread randomly by up to `jitter` of its length in both directions, so replicas that failed together do not retry
 * together. The policy only computes delays and keeps no state, so one instance serves every task that shares it. Application
 * events and processors use the same model for their retries.
 *
 * ```scala
 * val policy = RetryPolicy(maxAttempts = 3, initialDelay = 100.millis, maxDelay = 1.second, multiplier = 2.0, jitter = 0.0)
 * policy.delayAfter(1) // Some(100 milliseconds)
 * policy.delayAfter(2) // Some(200 milliseconds)
 * policy.delayAfter(3) // None, no tries left
 * ```
 *
 * @param maxAttempts
 *   the number of tries including the first one, at least 1
 * @param initialDelay
 *   the wait after the first failed try
 * @param maxDelay
 *   the upper limit of every wait, not less than `initialDelay`
 * @param multiplier
 *   how much each wait grows over the previous one, at least 1
 * @param jitter
 *   the part of a wait by which it is spread randomly, from 0 to 1
 * @throws java.lang.IllegalArgumentException
 *   if a parameter is out of its range
 */
final case class RetryPolicy(
  maxAttempts:  Int,
  initialDelay: FiniteDuration,
  maxDelay:     FiniteDuration,
  multiplier:   Double,
  jitter:       Double
) {
  require(maxAttempts >= 1, s"maxAttempts must be at least 1, got $maxAttempts")
  require(initialDelay >= Duration.Zero, s"initialDelay must not be negative, got $initialDelay")
  require(maxDelay >= initialDelay, s"maxDelay $maxDelay must not be less than initialDelay $initialDelay")
  require(multiplier >= 1.0, s"multiplier must be at least 1, got $multiplier")
  require(jitter >= 0.0 && jitter <= 1.0, s"jitter must be between 0 and 1, got $jitter")

  /** Whether the work may be tried again after `attempts` failed tries. */
  def canRetry(attempts: Int): Boolean = attempts < maxAttempts

  /**
   * The wait before the next try after `attempts` failed tries, with a random spread, or `None` when no tries are left.
   */
  def delayAfter(attempts: Int): Option[FiniteDuration] =
    delayAfter(attempts, ThreadLocalRandom.current.nextDouble())

  /**
   * The wait before the next try after `attempts` failed tries, or `None` when no tries are left.
   *
   * @param random
   *   a number from 0 to 1 that picks the spread: 0 shortens the wait by `jitter`, 0.5 keeps it and 1 lengthens it by `jitter`
   */
  def delayAfter(attempts: Int, random: Double): Option[FiniteDuration] =
    Option.when(attempts >= 1 && canRetry(attempts))(spread(baseDelay(attempts), random))

  private def baseDelay(attempts: Int): Double =
    math.min(initialDelay.toNanos * math.pow(multiplier, (attempts - 1).toDouble), maxDelay.toNanos.toDouble)

  private def spread(nanos: Double, random: Double): FiniteDuration =
    Duration.fromNanos(math.min(nanos * (1.0 + jitter * (2.0 * random - 1.0)), maxDelay.toNanos.toDouble).toLong)
}

/** Ready-made retry policies. */
object RetryPolicy {

  /** Five tries with waits from 1 second up to 1 minute, doubled each time and spread by 20%. */
  val default: RetryPolicy = RetryPolicy(5, 1.second, 1.minute, 2.0, 0.2)

  /** A single try without retries. */
  val none: RetryPolicy = RetryPolicy(1, Duration.Zero, Duration.Zero, 1.0, 0.0)

  /** `maxAttempts` tries with the same `delay` between them and no spread. */
  def fixed(maxAttempts: Int, delay: FiniteDuration): RetryPolicy =
    RetryPolicy(maxAttempts, delay, delay, 1.0, 0.0)
}
