package dev.coilix.diagnostics

/**
 * Finds platform threads that wait for each other forever, on monitors or on locks of `java.util.concurrent`, as
 * `ThreadMXBean.findDeadlockedThreads` reports them. The finding is `Critical`, and the diagnostics monitor reacts to it as
 * `coilix.diagnostics.threads.on-deadlock` says.
 */
final class DeadlockCheck extends ThreadCheck {

  override def check(sample: ThreadSample, history: ThreadHistory): List[Finding] = {
    val involved = sample.threads.filter(thread => sample.deadlocked.contains(thread.id))
    if (sample.deadlocked.isEmpty) Nil else List(findingOf(involved, sample.deadlocked.size))
  }

  private def findingOf(involved: List[ThreadSnapshot], count: Int): Finding =
    Finding(
      FindingSeverity.Critical,
      Finding.deadlock,
      s"$count threads are deadlocked: ${involved.map(_.name).sorted.mkString(", ")}",
      involved
    )
}
