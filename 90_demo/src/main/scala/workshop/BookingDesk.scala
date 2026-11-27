package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.annotation.Slf4s
import dev.coilix.diagnostics.HealthChanged
import dev.coilix.diagnostics.HealthKind
import dev.coilix.diagnostics.HealthStatus
import java.util.concurrent.atomic.AtomicBoolean

/** Takes bookings of new jobs, and stops while the workshop is not ready, as told by [[dev.coilix.diagnostics.HealthChanged]]. */
@Component
@Slf4s
final class BookingDesk {
  private val open = AtomicBoolean(true)

  /** Whether the desk takes new bookings now. */
  def accepting: Boolean = open.get

  @EventListener
  private def onHealthChanged(event: HealthChanged): IO[Unit] =
    IO.whenA(event.kind == HealthKind.Readiness)(
      IO(open.set(event.current == HealthStatus.Up)) *>
        logF.info(s"Bookings are ${if (event.current == HealthStatus.Up) "open" else s"paused: ${event.reasons.mkString(", ")}"}")
    )
}
