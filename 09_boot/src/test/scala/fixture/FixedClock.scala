package io.github.coilix.boot.fixture

import io.github.coilix.annotation.Component

@Component
final class FixedClock extends Clock {
  override def now: Long = 42L
}
