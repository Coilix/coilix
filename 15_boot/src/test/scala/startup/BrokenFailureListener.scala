package dev.coilix.boot
package startup

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener
import dev.coilix.events.ApplicationFailed

@Component
final class BrokenFailureListener {

  @EventListener
  private def onFailed(event: ApplicationFailed): Unit = throw IllegalStateException("cannot report")
}
