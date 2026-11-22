package dev.coilix.events
package invalid

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class ReturnsValue {
  @EventListener
  def onPlaced(event: OrderEvent.Placed): Int = event.id
}
