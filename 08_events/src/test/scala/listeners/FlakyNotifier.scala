package dev.coilix.events
package listeners

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
  def onPlaced(event: OrderEvent.Placed): Unit =
    if (attempts.incrementAndGet() < 3) throw IllegalStateException("not yet")
    else delivered.countDown()
}
