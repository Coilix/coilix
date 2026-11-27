package dev.coilix.diagnostics

import cats.effect.IO

/**
 * A bean that tells the state of one part of the application. The [[dev.coilix.diagnostics.Health]] bean collects every
 * indicator in the context.
 *
 * ```scala
 * @Component
 * final class QueueHealth(queue: OrderQueue) extends HealthIndicator {
 *   override def name: String = "orders"
 *
 *   override def kind: HealthKind = HealthKind.Readiness
 *
 *   override def state: IO[HealthState] =
 *     queue.size.map(size => if (size < 10000) HealthState.up else HealthState.degraded(s"$size orders are waiting"))
 * }
 * ```
 */
trait HealthIndicator {

  /** The name of the indicator, unique in the context, such as `threads`. */
  def name: String

  /** Whether the indicator counts for liveness or only for readiness. */
  def kind: HealthKind

  /**
   * The current state. It runs on the `coilix-diagnostics` thread every round, so it must be cheap; a call that waits goes in
   * `IO.blocking`. A failed effect counts as `Down` with the failure as the reason.
   */
  def state: IO[HealthState]
}
