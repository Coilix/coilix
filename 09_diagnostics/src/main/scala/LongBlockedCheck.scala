package dev.coilix.diagnostics

import java.time.Duration
import scala.concurrent.duration.FiniteDuration

/**
 * Reports, as a `Warning`, platform threads that have been `BLOCKED` on a monitor for at least `threshold`, measured from the
 * first sample that saw them blocked.
 */
final class LongBlockedCheck(threshold: FiniteDuration) extends ThreadCheck {

  override def check(sample: ThreadSample, history: ThreadHistory): List[Finding] = {
    val blocked = sample.threads.filter(thread => blockedLong(thread, sample, history))
    if (blocked.isEmpty) Nil else List(findingOf(blocked))
  }

  private def blockedLong(thread: ThreadSnapshot, sample: ThreadSample, history: ThreadHistory): Boolean =
    thread.state == Thread.State.BLOCKED && history.states.get(thread.id).exists(state => longEnough(state, sample))

  private def longEnough(state: StateSince, sample: ThreadSample): Boolean =
    state.state == Thread.State.BLOCKED && Duration.between(state.since, sample.at).toMillis >= threshold.toMillis

  private def findingOf(blocked: List[ThreadSnapshot]): Finding =
    Finding(
      FindingSeverity.Warning,
      Finding.longBlocked,
      s"${blocked.size} threads have been blocked on a monitor for at least $threshold: ${blocked.map(_.name).sorted.mkString(", ")}",
      blocked
    )
}
