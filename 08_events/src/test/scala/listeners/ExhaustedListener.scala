package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent
import java.util.concurrent.atomic.AtomicInteger

@Component
final class ExhaustedListener {
  val attempts: AtomicInteger = AtomicInteger()

  @EventListener(async = true)
  private def onPlaced(event: OrderEvent.Placed): Unit = {
    val _ = attempts.incrementAndGet()
    throw OutOfMemoryError("simulated")
  }
}
