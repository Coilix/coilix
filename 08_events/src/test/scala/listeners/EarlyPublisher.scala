package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.events.model.OrderEvent

@Component
final class EarlyPublisher(publisher: EventPublisher) {
  publisher.publish(OrderEvent.Placed(0))
}
