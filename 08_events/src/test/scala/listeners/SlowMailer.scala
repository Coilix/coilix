package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Component
final class SlowMailer(server: MailServer) {
  val started: CountDownLatch = CountDownLatch(1)

  val release: CountDownLatch = CountDownLatch(1)

  val sent: Recorder[(Int, Boolean)] = Recorder()

  @EventListener(async = true)
  def onPlaced(event: OrderEvent.Placed): IO[Unit] =
    IO(started.countDown()) *> IO.interruptible(release.await(10, TimeUnit.SECONDS)) *> IO(sent.record((event.id, server.isOpen)))
}
