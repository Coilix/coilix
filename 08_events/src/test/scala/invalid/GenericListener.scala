package dev.coilix.events
package invalid

import cats.effect.IO
import dev.coilix.annotation.EventListener

final class GenericListener[T] {
  @EventListener
  def onAny(event: T): IO[Unit] = IO.unit
}
