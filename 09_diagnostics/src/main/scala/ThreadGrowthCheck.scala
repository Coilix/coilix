package dev.coilix.diagnostics

import java.time.Duration
import scala.concurrent.duration.FiniteDuration

/**
 * Reports, as a `Warning`, a growth of the platform thread count by at least `limit` within the window the history keeps,
 * which suggests that threads leak. Nothing is reported within `grace` after the first sample, while the application starts
 * its pools.
 */
final class ThreadGrowthCheck(limit: Int, grace: FiniteDuration) extends ThreadCheck {

  override def check(sample: ThreadSample, history: ThreadHistory): List[Finding] =
    if (afterGrace(sample, history)) growthOf(history.counts).filter(_ >= limit).toList.map(findingOf(_, history)) else Nil

  private def afterGrace(sample: ThreadSample, history: ThreadHistory): Boolean =
    history.firstSample.exists(first => Duration.between(first, sample.at).toMillis >= grace.toMillis)

  private def growthOf(counts: List[CountPoint]): Option[Int] =
    counts.headOption.zip(counts.lastOption).map((first, last) => last.count - first.count)

  private def findingOf(growth: Int, history: ThreadHistory): Finding = {
    val counts = history.counts.map(_.count)
    Finding(
      FindingSeverity.Warning,
      Finding.threadGrowth,
      s"The platform thread count grew by $growth since ${history.counts.head.at} (now ${counts.last}, lowest ${counts.min})",
      Nil
    )
  }
}
