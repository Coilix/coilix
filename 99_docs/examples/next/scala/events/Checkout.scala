package com.example
package events

import cats.effect.IO
import dev.coilix.annotation.Service
import dev.coilix.events.EventPublisher

@Service
final class Checkout(events: EventPublisher) {
  def place(orderId: String, customer: String, total: BigDecimal): IO[Unit] =
    events.publish(OrderPlaced(orderId, customer, total))
}
