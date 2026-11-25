package dev.coilix.diagnostics

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/**
 * What earlier samples saw, for checks that need time: how long each thread has been in its state and how the number of
 * threads changed.
 *
 * @param states
 *   the state of each live thread with the time it entered it, by thread identifier
 * @param counts
 *   the thread counts within the kept window, oldest first
 * @param firstSample
 *   the time of the first sample, empty before it
 */
final case class ThreadHistory(states: Map[Long, StateSince], counts: List[CountPoint], firstSample: Option[Instant]) {

  /**
   * The history after `sample`: threads that are gone are forgotten, a thread keeps its time while its state stays the same,
   * and counts older than `window` are dropped.
   */
  def next(sample: ThreadSample, window: FiniteDuration): ThreadHistory = {
    val cutoff = sample.at.minusMillis(window.toMillis)
    ThreadHistory(
      sample.threads.map(thread => thread.id -> stateOf(thread, sample.at)).toMap,
      (counts :+ CountPoint(sample.at, sample.threadCount)).filterNot(_.at.isBefore(cutoff)),
      firstSample.orElse(Some(sample.at))
    )
  }

  private def stateOf(thread: ThreadSnapshot, at: Instant): StateSince =
    states.get(thread.id).filter(_.state == thread.state).getOrElse(StateSince(thread.state, at))
}

/** The starting [[dev.coilix.diagnostics.ThreadHistory]]. */
object ThreadHistory {

  /** The history before the first sample. */
  val empty: ThreadHistory = ThreadHistory(Map.empty, Nil, None)
}
