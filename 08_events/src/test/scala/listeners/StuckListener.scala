package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@Component
final class StuckListener {
  val started: CountDownLatch = CountDownLatch(1)

  val cancelled: AtomicBoolean = AtomicBoolean(false)

  @EventListener(async = true)
  def onPlaced(event: OrderEvent.Placed): IO[Unit] =
    IO(started.countDown()) *> IO.never[Unit].onCancel(IO(cancelled.set(true)))
}
