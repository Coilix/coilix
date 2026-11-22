package com.example
package events

import dev.coilix.annotation.Service
import dev.coilix.events.EventPublisher
import dev.coilix.events.Events

@Service
final class ExpressCheckout(using EventPublisher) {
  def place(orderId: String, customer: String): Unit =
    Events.publish(OrderPlaced(orderId, customer, BigDecimal(0)))
}
