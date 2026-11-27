package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class AsyncJournal {
  val orders: Recorder[Int] = Recorder()

  @EventListener(async = true)
  private def onPlaced(event: OrderEvent.Placed): IO[Unit] = IO(orders.record(event.id))
}
