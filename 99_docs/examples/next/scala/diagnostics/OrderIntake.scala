package com.example
package diagnostics

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.diagnostics.HealthChanged
import dev.coilix.diagnostics.HealthKind
import dev.coilix.diagnostics.HealthStatus
import java.util.concurrent.atomic.AtomicBoolean

@Component
final class OrderIntake {
  private val open = AtomicBoolean(true)

  def accepting: Boolean = open.get

  @EventListener
  private def onHealthChanged(event: HealthChanged): IO[Unit] =
    IO.whenA(event.kind == HealthKind.Readiness)(IO(open.set(event.current == HealthStatus.Up)))
}
