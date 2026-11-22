package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.OrderEvent

@Component
final class AsyncJournal {
  val threads: Recorder[String] = Recorder()

  @EventListener(async = true)
  private def onPlaced(event: OrderEvent.Placed): Unit = threads.record(Thread.currentThread.getName)
}
