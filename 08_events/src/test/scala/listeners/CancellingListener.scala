package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class CancellingListener(publisher: EventPublisher) {
  @EventListener(async = true)
  def onPlaced(event: OrderEvent.Placed): Unit = {
    Thread.sleep(100)
    publisher.publish(OrderEvent.Cancelled(event.id))
  }
}
