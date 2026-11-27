package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.annotation.Lazy
import dev.coilix.events.model.OrderEvent

@Component
@Lazy
final class LazyAuditor {
  val events: Recorder[OrderEvent] = Recorder()

  @EventListener
  private def onOrder(event: OrderEvent): IO[Unit] = IO(events.record(event))
}
