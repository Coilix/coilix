package dev.coilix.processing

import java.time.Duration as JavaDuration
import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.DurationLong
import scala.concurrent.duration.FiniteDuration

/**
 * How a shard's worker is restarted after a failure, set under `coilix.processors.<name>.restart`.
 *
 * Every failure waits for a delay from `backoff` before the next start. When the shard fails more than `maxAttempts`
 * times within `within`, it gives up and stays `Failed` until it is started again through the
 * [[dev.coilix.processing.ProcessorManager]]. A worker that ran for `resetAfter` without failing starts counting again,
 * so rare failures over days never exhaust the attempts.
 */
final case class RestartPolicy(
  backoff:     Backoff        = Backoff(),
  maxAttempts: Int            = 10,
  within:      FiniteDuration = 15.minutes,
  resetAfter:  FiniteDuration = 5.minutes
) {
  require(maxAttempts >= 0, s"maxAttempts must not be negative, got $maxAttempts")

  /**
   * Decides what follows a failure at `now` of a worker that has been running since `runningSince`.
   *
   * @param failures
   *   the earlier failures that still count, newest first
   * @param random
   *   a number from 0 to 1 for the jitter of the backoff
   * @return
   *   the failures that count from now on, newest first, and the decision
   */
  def afterFailure(
    failures:     List[Instant],
    runningSince: Instant,
    now:          Instant,
    random:       Double
  ): (List[Instant], RestartDecision) = {
    val counted  = now :: recent(failures, runningSince, now)
    val decision =
      if (counted.size > maxAttempts) RestartDecision.GiveUp else RestartDecision.Restart(backoff.delay(counted.size, random))
    (counted, decision)
  }

  private def recent(failures: List[Instant], runningSince: Instant, now: Instant): List[Instant] =
    if (elapsed(runningSince, now) >= resetAfter) Nil else failures.filter(failure => elapsed(failure, now) < within)

  private def elapsed(from: Instant, to: Instant): FiniteDuration =
    JavaDuration.between(from, to).toNanos.max(0L).nanos
}
