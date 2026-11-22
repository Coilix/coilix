package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent
import java.util.concurrent.CountDownLatch

@Component
final class ArchivingListener {
  val threads: Recorder[String] = Recorder()

  val archived: CountDownLatch = CountDownLatch(1)

  @EventListener(async = true, blocking = true)
  private def onPlaced(event: OrderEvent.Placed): Unit = {
    threads.record(Thread.currentThread.getName)
    archived.countDown()
  }
}
