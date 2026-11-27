package dev.coilix.boot
package startup

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.ApplicationFailed

@Component
final class BrokenFailureListener {

  @EventListener
  private def onFailed(event: ApplicationFailed): IO[Unit] = IO.raiseError(IllegalStateException("cannot report"))
}
