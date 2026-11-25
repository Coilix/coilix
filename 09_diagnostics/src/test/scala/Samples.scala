package dev.coilix.diagnostics

import java.time.Instant

/** Thread samples built by hand for tests of checks. */
object Samples {

  /** The time of the first sample in tests. */
  val start: Instant = Instant.parse("2026-11-25T10:00:00Z")

  /** A platform thread with `id`, `name` and `state`, without a stack or a lock. */
  def thread(id: Long, name: String, state: Thread.State): ThreadSnapshot =
    ThreadSnapshot(id, name, state, daemon = false, Nil, None, None)

  /** A sample at `seconds` after [[start]] with `threads` and `count` live threads. */
  def at(seconds: Long, threads: List[ThreadSnapshot], count: Int, deadlocked: Set[Long] = Set.empty): ThreadSample =
    ThreadSample(start.plusSeconds(seconds), count, 0, threads, deadlocked)
}
