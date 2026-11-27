package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.events.model.OrderEvent

@Component
final class EarlyPublisher(publisher: EventPublisher) {

  @PostConstruct
  def announce(): IO[Unit] = publisher.publish(OrderEvent.Placed(0))
}
