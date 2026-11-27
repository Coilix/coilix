package dev.coilix.events
package invalid

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class TwoParameters {
  @EventListener
  def onPlaced(event: OrderEvent.Placed, other: OrderEvent.Placed): IO[Unit] = IO.unit
}
