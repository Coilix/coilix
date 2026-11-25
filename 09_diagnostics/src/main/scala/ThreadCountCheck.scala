package dev.coilix.diagnostics

/**
 * Reports more live platform threads than the limits: a `Warning` from `warningCount` and a `Critical` finding from
 * `criticalCount`. Virtual threads do not count.
 */
final class ThreadCountCheck(warningCount: Int, criticalCount: Int) extends ThreadCheck {

  override def check(sample: ThreadSample, history: ThreadHistory): List[Finding] =
    severityOf(sample.threadCount).toList.map(severity =>
      Finding(severity, Finding.tooManyThreads, s"${sample.threadCount} platform threads are alive", Nil)
    )

  private def severityOf(count: Int): Option[FindingSeverity] =
    if (count >= criticalCount) Some(FindingSeverity.Critical)
    else Option.when(count >= warningCount)(FindingSeverity.Warning)
}
