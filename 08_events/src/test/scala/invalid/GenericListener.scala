package dev.coilix.events
package invalid

import dev.coilix.annotation.EventListener

final class GenericListener[T] {
  @EventListener
  def onAny(event: T): Unit = ()
}
