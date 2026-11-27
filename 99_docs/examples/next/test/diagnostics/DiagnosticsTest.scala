package com.example
package diagnostics

import cats.effect.IO
import dev.coilix.diagnostics.Health
import dev.coilix.diagnostics.HealthStatus
import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite

final class DiagnosticsTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[DiagnosticsApplication.type].withSynchronousEvents

  coilixTest("a long stock backlog makes the shop not ready and pauses the order intake") {
    val health = bean[Health]
    for {
      _      <- IO(bean[StockSync].queue(1500)) *> health.refresh()
      backlog = (health.readiness, health.liveness, bean[OrderIntake].accepting)
      _      <- IO(bean[StockSync].synced(1500)) *> health.refresh()
    } yield {
      assertEquals(backlog, (HealthStatus.Degraded, HealthStatus.Up, false))
      assertEquals((health.readiness, bean[OrderIntake].accepting), (HealthStatus.Up, true))
    }
  }
}
