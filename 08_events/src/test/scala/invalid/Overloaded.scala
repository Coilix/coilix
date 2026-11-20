package dev.coilix.events
package invalid

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class Overloaded {
  @EventListener
  def on(event: OrderEvent.Placed): Unit = ()

  @EventListener
  def on(event: OrderEvent.Cancelled): Unit = ()
}
