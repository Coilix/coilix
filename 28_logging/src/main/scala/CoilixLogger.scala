package dev.coilix.logging

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.MDC
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.AbstractLogger
import org.slf4j.helpers.MessageFormatter
import scala.jdk.CollectionConverters.*

/**
 * The SLF4J logger of the `logging` module. Its level and the appender are shared with the
 * [[dev.coilix.logging.CoilixLoggerFactory]], which changes them when the configuration or a runtime override changes. The
 * references are atomic because SLF4J calls the logger from any thread without an effect.
 *
 * @param loggerName
 *   the full name of the logger
 * @param level
 *   the current level of the logger
 * @param appender
 *   the appender of every logger of the factory
 */
case class CoilixLogger(loggerName: String, level: AtomicReference[LogLevel], appender: AtomicReference[LogAppender])
    extends AbstractLogger {

  override def getName: String = loggerName

  override def isTraceEnabled: Boolean = enabled(LogLevel.Trace)

  override def isTraceEnabled(marker: Marker): Boolean = isTraceEnabled

  override def isDebugEnabled: Boolean = enabled(LogLevel.Debug)

  override def isDebugEnabled(marker: Marker): Boolean = isDebugEnabled

  override def isInfoEnabled: Boolean = enabled(LogLevel.Info)

  override def isInfoEnabled(marker: Marker): Boolean = isInfoEnabled

  override def isWarnEnabled: Boolean = enabled(LogLevel.Warn)

  override def isWarnEnabled(marker: Marker): Boolean = isWarnEnabled

  override def isErrorEnabled: Boolean = enabled(LogLevel.Error)

  override def isErrorEnabled(marker: Marker): Boolean = isErrorEnabled

  override protected def getFullyQualifiedCallerName: String = classOf[CoilixLogger].getName

  override protected def handleNormalizedLoggingCall(
    eventLevel: Level,
    marker:     Marker,
    pattern:    String,
    arguments:  Array[AnyRef],
    error:      Throwable
  ): Unit =
    appender.get.append(
      LogEvent(
        Instant.now,
        Thread.currentThread.getName,
        LogLevel.of(eventLevel),
        loggerName,
        MessageFormatter.basicArrayFormat(pattern, arguments),
        Option(error),
        contextMap
      )
    )

  private def enabled(message: LogLevel): Boolean = level.get.enables(message)

  private def contextMap: Map[String, String] =
    Option(MDC.getCopyOfContextMap).fold(Map.empty[String, String])(_.asScala.toMap)
}
