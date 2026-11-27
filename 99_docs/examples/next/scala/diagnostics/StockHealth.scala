package com.example
package diagnostics

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.diagnostics.HealthIndicator
import dev.coilix.diagnostics.HealthKind
import dev.coilix.diagnostics.HealthState

@Component
final class StockHealth(sync: StockSync) extends HealthIndicator {

  override def name: String = "stock"

  override def kind: HealthKind = HealthKind.Readiness

  override def state: IO[HealthState] =
    IO(sync.backlog).map(backlog =>
      if (backlog < 1000) HealthState.up else HealthState.degraded(s"$backlog stock changes wait for the warehouse")
    )
}
