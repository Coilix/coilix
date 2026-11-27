package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class OrderJournal {
  val placed: Recorder[OrderEvent.Placed] = Recorder()

  @EventListener
  private def onPlaced(event: OrderEvent.Placed): IO[Unit] = IO(placed.record(event))
}
