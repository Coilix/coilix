package dev.coilix.events
package listeners

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

  val threads: Recorder[String] = Recorder()

  @EventListener(async = true)
  def onPlaced(event: OrderEvent.Placed): Unit = {
    threads.record(Thread.currentThread.getName)
    started.countDown()
    val _ = release.await(10, TimeUnit.SECONDS)
    sent.record((event.id, server.isOpen))
  }
}
