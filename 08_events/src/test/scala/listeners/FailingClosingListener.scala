package dev.coilix.events
package listeners

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener

@Component
final class FailingClosingListener {

  @EventListener
  private def onClosing(event: ContextClosing): IO[Unit] = IO.raiseError(IllegalStateException("cannot say goodbye"))
}
