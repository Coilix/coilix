package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Profile

/** Drops notifications. Used when the `dev` profile is not active. */
@Component
@Profile(Array("!dev"))
final class SilentNotifier extends Notifier {
  override def description: String = "none"

  override def send(message: String): Unit = ()
}
