package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@Component
final class StuckListener {
  val started: CountDownLatch = CountDownLatch(1)

  val interrupted: AtomicBoolean = AtomicBoolean(false)

  @EventListener(async = true)
  def onPlaced(event: OrderEvent.Placed): Unit = {
    started.countDown()
    try {
      CountDownLatch(1).await()
    } catch {
      case failure: InterruptedException =>
        interrupted.set(true)
        throw failure
    }
  }
}
