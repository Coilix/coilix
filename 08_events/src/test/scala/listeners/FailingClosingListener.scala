package dev.coilix.events
package listeners

import dev.coilix.annotation.Component
import dev.coilix.annotation.EventListener

@Component
final class FailingClosingListener {

  @EventListener
  private def onClosing(event: ContextClosing): Unit = throw IllegalStateException("cannot say goodbye")
}
