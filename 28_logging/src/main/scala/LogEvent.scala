package dev.coilix.logging

import java.time.Instant

/**
 * One message on its way to the [[dev.coilix.logging.LogAppender]].
 *
 * @param time
 *   when the message was logged
 * @param thread
 *   the name of the thread that logged it
 * @param level
 *   its level
 * @param logger
 *   the full name of the logger, usually the class
 * @param message
 *   the message with its arguments filled in
 * @param error
 *   the exception logged with it
 * @param context
 *   the MDC of the thread at the time of the call
 */
final case class LogEvent(
  time:    Instant,
  thread:  String,
  level:   LogLevel,
  logger:  String,
  message: String,
  error:   Option[Throwable],
  context: Map[String, String]
)
