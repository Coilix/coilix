package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.model.Changed
import dev.coilix.events.model.Customer

@Component
final class CustomerWatcher {
  val changes: Recorder[Changed[Customer]] = Recorder()

  @EventListener
  def onCustomerChanged(event: Changed[Customer]): IO[Unit] = IO(changes.record(event))
}
