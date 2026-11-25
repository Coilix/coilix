package com.example
package diagnostics

import dev.coilix.annotation.Component
import dev.coilix.diagnostics.HealthIndicator
import dev.coilix.diagnostics.HealthKind
import dev.coilix.diagnostics.HealthState

@Component
final class StockHealth(sync: StockSync) extends HealthIndicator {

  override def name: String = "stock"

  override def kind: HealthKind = HealthKind.Readiness

  override def state: HealthState =
    if (sync.backlog < 1000) HealthState.up
    else HealthState.degraded(s"${sync.backlog} stock changes wait for the warehouse")
}
