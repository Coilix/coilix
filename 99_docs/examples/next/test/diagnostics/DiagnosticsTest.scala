package com.example
package diagnostics

import dev.coilix.diagnostics.Health
import dev.coilix.diagnostics.HealthStatus
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup

final class DiagnosticsTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[DiagnosticsApplication.type].withSynchronousEvents

  test("a long stock backlog makes the shop not ready and pauses the order intake") {
    setup.use {
      bean[StockSync].queue(1500)
      bean[Health].refresh()
      assertEquals((bean[Health].readiness, bean[Health].liveness), (HealthStatus.Degraded, HealthStatus.Up))
      assertEquals(bean[OrderIntake].accepting, false)
      bean[StockSync].synced(1500)
      bean[Health].refresh()
      assertEquals((bean[Health].readiness, bean[OrderIntake].accepting), (HealthStatus.Up, true))
    }
  }
}
