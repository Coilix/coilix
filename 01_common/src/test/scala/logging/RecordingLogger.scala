package dev.coilix.common
package logging

import java.util.concurrent.atomic.AtomicReference
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.AbstractLogger

/** A logger that records what it logs, with only the given levels enabled. */
final class RecordingLogger(enabled: Set[Level]) extends AbstractLogger {
  private val entries = AtomicReference(Vector.empty[String])

  def recorded: Vector[String] = entries.get

  override def isTraceEnabled: Boolean = enabled(Level.TRACE)

  override def isTraceEnabled(marker: Marker): Boolean = isTraceEnabled

  override def isDebugEnabled: Boolean = enabled(Level.DEBUG)

  override def isDebugEnabled(marker: Marker): Boolean = isDebugEnabled

  override def isInfoEnabled: Boolean = enabled(Level.INFO)

  override def isInfoEnabled(marker: Marker): Boolean = isInfoEnabled

  override def isWarnEnabled: Boolean = enabled(Level.WARN)

  override def isWarnEnabled(marker: Marker): Boolean = isWarnEnabled

  override def isErrorEnabled: Boolean = enabled(Level.ERROR)

  override def isErrorEnabled(marker: Marker): Boolean = isErrorEnabled

  override protected def getFullyQualifiedCallerName: String = classOf[RecordingLogger].getName

  override protected def handleNormalizedLoggingCall(
    level:     Level,
    marker:    Marker,
    message:   String,
    arguments: Array[AnyRef],
    error:     Throwable
  ): Unit = {
    val suffix = Option(error).fold("")(failure => s" (${failure.getMessage})")
    entries.getAndUpdate(_ :+ s"$level $message$suffix"): Unit
  }
}
