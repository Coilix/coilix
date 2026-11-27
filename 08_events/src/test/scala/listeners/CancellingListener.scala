package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent
import scala.concurrent.duration.DurationInt

@Component
final class CancellingListener(publisher: EventPublisher) {
  @EventListener(async = true)
  def onPlaced(event: OrderEvent.Placed): IO[Unit] =
    IO.sleep(100.millis) *> publisher.publish(OrderEvent.Cancelled(event.id))
}
