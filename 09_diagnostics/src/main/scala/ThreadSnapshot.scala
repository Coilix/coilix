package dev.coilix.diagnostics

/**
 * One platform thread in a [[dev.coilix.diagnostics.ThreadSample]].
 *
 * @param state
 *   the state from `Thread.getState`, which stays right also when the thread waits for a monitor held by a virtual thread
 * @param stack
 *   the first frames of the stack
 * @param lock
 *   the lock the thread waits for, if any
 * @param lockOwner
 *   the name of the thread that holds `lock`, if known
 */
final case class ThreadSnapshot(
  id:        Long,
  name:      String,
  state:     Thread.State,
  daemon:    Boolean,
  stack:     List[StackTraceElement],
  lock:      Option[String],
  lockOwner: Option[String]
)
