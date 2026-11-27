package dev.coilix.events
package invalid

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class Overloaded {
  @EventListener
  def on(event: OrderEvent.Placed): IO[Unit] = IO.unit

  @EventListener
  def on(event: OrderEvent.Cancelled): IO[Unit] = IO.unit
}
