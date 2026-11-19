package dev.coilix.boot
package fixture

import dev.coilix.annotation.Component

@Component
final class FixedClock extends Clock {
  override def now: Long = 42L
}
