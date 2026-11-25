package dev.coilix.diagnostics

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
 *   override def state: HealthState =
 *     if (queue.size < 10000) HealthState.up else HealthState.degraded(s"${queue.size} orders are waiting")
 * }
 * ```
 */
trait HealthIndicator {

  /** The name of the indicator, unique in the context, such as `threads`. */
  def name: String

  /** Whether the indicator counts for liveness or only for readiness. */
  def kind: HealthKind

  /** The current state. It is read on the diagnostics thread, so it must be cheap and must not block for long. */
  def state: HealthState
}
