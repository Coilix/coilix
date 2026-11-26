package dev.coilix.logging

import org.slf4j.event.Level

/** The level of a logger: messages below it are dropped, and `Off` drops every message. */
enum LogLevel(val severity: Int, val label: String) {

  /** Fine-grained steps, for tracing a problem. */
  case Trace extends LogLevel(0, "TRACE")

  /** Details useful while developing or diagnosing. */
  case Debug extends LogLevel(1, "DEBUG")

  /** Normal events of the application. */
  case Info extends LogLevel(2, "INFO")

  /** Something unexpected that the application survives. */
  case Warn extends LogLevel(3, "WARN")

  /** A failure. */
  case Error extends LogLevel(4, "ERROR")

  /** No messages at all, which silences the logger. */
  case Off extends LogLevel(5, "OFF")

  /** Whether a logger at this level logs a message at `message`. */
  def enables(message: LogLevel): Boolean = this != Off && message.severity >= severity
}

/** Conversions of [[dev.coilix.logging.LogLevel]]. */
object LogLevel {

  /** The level named `name`, ignoring case, such as `warn` or `OFF`. */
  def parse(name: String): Option[LogLevel] =
    values.find(_.label.equalsIgnoreCase(name.trim))

  /** The level of an SLF4J event. */
  def of(level: Level): LogLevel =
    level match {
      case Level.TRACE => Trace
      case Level.DEBUG => Debug
      case Level.INFO  => Info
      case Level.WARN  => Warn
      case Level.ERROR => Error
    }
}
