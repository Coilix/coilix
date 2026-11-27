package dev.coilix.testkit
package fixture

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.DurationInt

@Component
final class GuestBook {
  private val entries = AtomicReference(List.empty[(String, String)])

  def guests: List[String] = entries.get.map(_._1)

  def threads: List[String] = entries.get.map(_._2)

  @EventListener(async = true)
  private def onVisit(event: Visit): IO[Unit] =
    IO.sleep(50.millis) *> IO(entries.updateAndGet(_ :+ (event.guest, Thread.currentThread.getName))).void
}
