package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.diagnostics.HealthIndicator
import dev.coilix.diagnostics.HealthKind
import dev.coilix.diagnostics.HealthState

/**
 * The `lifts` readiness indicator: the workshop is `Degraded` while every lift holds a car, so it should not take new jobs.
 * The `health` bean of the diagnostics module reads it on the `coilix-diagnostics` thread.
 */
@Component
final class LiftHealth(board: LiftBoard) extends HealthIndicator {

  override def name: String = "lifts"

  override def kind: HealthKind = HealthKind.Readiness

  override def state: HealthState =
    if (board.free > 0) HealthState.up else HealthState.degraded(s"all ${board.lifts} lifts are taken")
}
