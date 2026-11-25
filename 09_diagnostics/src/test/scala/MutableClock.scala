package dev.coilix.diagnostics

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.FiniteDuration

/** A clock that tests move forward. */
final class MutableClock(start: Instant) extends Clock {

  private val now = AtomicReference(start)

  override def getZone: ZoneId = ZoneOffset.UTC

  override def withZone(zone: ZoneId): Clock = this

  override def instant: Instant = now.get

  /** Moves the clock forward by `step`. */
  def advance(step: FiniteDuration): Unit = {
    val _ = now.updateAndGet(_.plusMillis(step.toMillis))
  }
}
