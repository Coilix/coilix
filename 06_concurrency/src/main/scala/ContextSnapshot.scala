package dev.coilix.concurrency

/** Thread-bound context taken by a [[dev.coilix.concurrency.ContextPropagator]], ready to be restored around a task. */
trait ContextSnapshot {

  /**
   * Runs `task` on the current thread with the captured context, and gives the thread its previous context back afterwards,
   * also when `task` fails.
   */
  def runWith[T](task: => T): T
}

/** Ready-made [[dev.coilix.concurrency.ContextSnapshot]]s. */
object ContextSnapshot {

  /** A snapshot that restores nothing. */
  val empty: ContextSnapshot = new ContextSnapshot {
    override def runWith[T](task: => T): T = task
  }

  /** A snapshot that restores `outer` around `inner`. */
  def nested(outer: ContextSnapshot, inner: ContextSnapshot): ContextSnapshot = new ContextSnapshot {
    override def runWith[T](task: => T): T = outer.runWith(inner.runWith(task))
  }
}
