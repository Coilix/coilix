package dev.coilix.diagnostics

/**
 * A problem found by a [[dev.coilix.diagnostics.ThreadCheck]].
 *
 * @param kind
 *   what was found, such as `deadlock`; repeated findings of one kind are reported less often
 * @param threads
 *   the threads involved, logged with their stacks
 */
final case class Finding(severity: FindingSeverity, kind: String, message: String, threads: List[ThreadSnapshot])

/** The kinds of the findings of the built-in checks. */
object Finding {

  /** Threads that wait for each other forever, found by [[dev.coilix.diagnostics.DeadlockCheck]]. */
  val deadlock: String = "deadlock"

  /** More platform threads than the configured limits, found by [[dev.coilix.diagnostics.ThreadCountCheck]]. */
  val tooManyThreads: String = "too-many-threads"

  /** Threads blocked on a monitor for long, found by [[dev.coilix.diagnostics.LongBlockedCheck]]. */
  val longBlocked: String = "long-blocked"

  /** A fast growth of the number of threads, found by [[dev.coilix.diagnostics.ThreadGrowthCheck]]. */
  val threadGrowth: String = "thread-growth"
}
