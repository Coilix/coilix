package dev.coilix.diagnostics

import java.time.Instant

/**
 * The platform threads of the JVM at one moment, as the [[dev.coilix.diagnostics.ThreadCheck]]s see them. Virtual threads are
 * not part of it, because the JVM management API does not report them.
 *
 * @param threadCount
 *   the number of live platform threads
 * @param deadlocked
 *   the identifiers of the threads that `ThreadMXBean.findDeadlockedThreads` reported
 */
final case class ThreadSample(
  at:          Instant,
  threadCount: Int,
  daemonCount: Int,
  threads:     List[ThreadSnapshot],
  deadlocked:  Set[Long]
)
