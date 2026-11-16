package dev.coilix.context
package conditions

final class SystemClock extends Clock {
  override def now: Long = 1L
}
