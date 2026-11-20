package com.example
package events

import dev.coilix.annotation.Service
import dev.coilix.events.EventPublisher

@Service
final class Checkout(events: EventPublisher) {
  def place(orderId: String, customer: String, total: BigDecimal): Unit =
    events.publish(OrderPlaced(orderId, customer, total))
}
