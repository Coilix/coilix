package dev.coilix.concurrency

/**
 * Carries a piece of thread-bound context, such as the logging MDC, from the thread that hands a task over to the thread that
 * runs it.
 *
 * The executors and the scheduler of the concurrency module capture the context of every [[dev.coilix.concurrency.ContextPropagator]]
 * bean when a task is submitted or scheduled, and run the task with it. The module registers a
 * [[dev.coilix.concurrency.MdcContextPropagator]]; an application adds its own propagators as beans:
 *
 * ```scala
 * @Component
 * final class TenantPropagator extends ContextPropagator {
 *   override def capture(): ContextSnapshot = {
 *     val tenant = Tenant.current.get
 *     new ContextSnapshot {
 *       override def runWith[T](task: => T): T = Tenant.withValue(tenant)(task)
 *     }
 *   }
 * }
 * ```
 */
trait ContextPropagator {

  /** Takes the context of the calling thread, to be restored around a task on another thread. */
  def capture(): ContextSnapshot
}

/** Capturing with several [[dev.coilix.concurrency.ContextPropagator]]s at once. */
object ContextPropagator {

  /** The context of every propagator in `propagators`, restored in this order, the first one outermost. */
  def captureAll(propagators: List[ContextPropagator]): ContextSnapshot =
    propagators.map(_.capture()).foldRight(ContextSnapshot.empty)(ContextSnapshot.nested)
}
