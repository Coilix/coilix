package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@Component
final class FlakyNotifier {
  val attempts: AtomicInteger = AtomicInteger()

  val delivered: CountDownLatch = CountDownLatch(1)

  @EventListener(async = true)
  def onPlaced(event: OrderEvent.Placed): IO[Unit] =
    IO(attempts.incrementAndGet()).flatMap(attempt =>
      if (attempt < 3) IO.raiseError(IllegalStateException("not yet")) else IO(delivered.countDown())
    )
}
