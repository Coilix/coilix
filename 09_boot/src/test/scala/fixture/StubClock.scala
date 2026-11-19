package dev.coilix.boot
package fixture

final class StubClock(time: Long) extends Clock {
  override def now: Long = time
}
