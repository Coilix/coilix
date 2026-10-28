package org.scaling.boot.fixture

import org.scaling.annotation.Component

@Component
final class FixedClock extends Clock {
  override def now: Long = 42L
}
