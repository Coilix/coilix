package dev.coilix.diagnostics

/**
 * A check of the platform threads that the diagnostics thread runs every `coilix.diagnostics.interval`. Every bean of this
 * type in the context takes part, so an application adds its own checks as beans.
 *
 * ```scala
 * @Component
 * final class ReportThreadsCheck extends ThreadCheck {
 *   override def check(sample: ThreadSample, history: ThreadHistory): List[Finding] =
 *     sample.threads.filter(_.name.startsWith("report-")).drop(4) match {
 *       case Nil   => Nil
 *       case extra => List(Finding(FindingSeverity.Warning, "report-threads", s"${extra.size} report threads too many", extra))
 *     }
 * }
 * ```
 */
trait ThreadCheck {

  /** The findings in `sample`. `history` already includes `sample`. */
  def check(sample: ThreadSample, history: ThreadHistory): List[Finding]
}
