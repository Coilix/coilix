package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class OrderJournal {
  val placed: Recorder[OrderEvent.Placed] = Recorder()

  val threads: Recorder[String] = Recorder()

  @EventListener
  private def onPlaced(event: OrderEvent.Placed): Unit = {
    threads.record(Thread.currentThread.getName)
    placed.record(event)
  }
}
