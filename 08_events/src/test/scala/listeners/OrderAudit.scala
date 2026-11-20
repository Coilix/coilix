package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.events.model.OrderEvent

@Component
final class OrderAudit extends ApplicationEventListener[OrderEvent] {
  val events: Recorder[OrderEvent] = Recorder()

  override def onEvent(event: OrderEvent): Unit = events.record(event)
}
