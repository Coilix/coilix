package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent
import java.util.concurrent.atomic.AtomicInteger

@Component
final class FailingNotifier {
  val attempts: AtomicInteger = AtomicInteger()

  @EventListener(async = true)
  def onPlaced(event: OrderEvent.Placed): IO[Unit] =
    IO(attempts.incrementAndGet()) *> IO.raiseError(IllegalStateException("always fails"))
}
