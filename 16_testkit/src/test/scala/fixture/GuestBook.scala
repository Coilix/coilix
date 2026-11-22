package dev.coilix.testkit
package fixture

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import java.util.concurrent.atomic.AtomicReference

@Component
final class GuestBook {
  private val entries = AtomicReference(List.empty[(String, String)])

  def guests: List[String] = entries.get.map(_._1)

  def threads: List[String] = entries.get.map(_._2)

  @EventListener(async = true)
  private def onVisit(event: Visit): Unit = {
    Thread.sleep(50)
    val _ = entries.updateAndGet(_ :+ (event.guest, Thread.currentThread.getName))
  }
}
