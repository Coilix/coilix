package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class StrictValidators {
  val warehouse: Recorder[OrderEvent.Cancelled] = Recorder()

  @EventListener
  def checkCustomer(event: OrderEvent.Cancelled): Unit = throw IllegalArgumentException("customer rejected")

  @EventListener
  def checkStock(event: OrderEvent.Cancelled): Unit = throw IllegalStateException("stock rejected")

  @EventListener(async = true)
  def notifyWarehouse(event: OrderEvent.Cancelled): Unit = warehouse.record(event)
}
