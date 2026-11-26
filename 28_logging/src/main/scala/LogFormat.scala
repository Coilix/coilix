package dev.coilix.logging

import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turns a [[dev.coilix.logging.LogEvent]] into the text of one log entry, followed by the stack trace of its exception:
 *
 * ```text
 * 2026-11-26 14:03:22.123 INFO  [io-compute-3] d.c.d.w.FaultCodeExport - Shard fault-code-export#0 exported 2 vehicles {shard=0}
 * ```
 *
 * @param colors
 *   whether the time and the level are marked with ANSI colors
 * @param zone
 *   the time zone of the timestamps
 */
case class LogFormat(colors: Boolean, zone: ZoneId) {

  private val timestamps = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(zone)

  /** The entry for `event`, without a trailing line separator. */
  def format(event: LogEvent): String =
    s"${colored(LogFormat.Green, timestamps.format(event.time))} ${levelOf(event.level)} [${event.thread}] " +
      s"${LoggerNames.shortened(event.logger)} - ${event.message}${contextOf(event.context)}${event.error.fold("")(stackOf)}"

  private def levelOf(level: LogLevel): String = {
    val padded = level.label.padTo(5, ' ')
    level match {
      case LogLevel.Error => colored(LogFormat.BoldRed, padded)
      case LogLevel.Warn  => colored(LogFormat.BoldYellow, padded)
      case LogLevel.Info  => colored(LogFormat.Blue, padded)
      case LogLevel.Debug => colored(LogFormat.Cyan, padded)
      case _              => padded
    }
  }

  private def contextOf(context: Map[String, String]): String =
    if (context.isEmpty) "" else context.toList.sorted.map((key, value) => s"$key=$value").mkString(" {", ", ", "}")

  private def stackOf(error: Throwable): String = {
    val text = StringWriter()
    error.printStackTrace(PrintWriter(text))
    System.lineSeparator + text.toString.stripTrailing
  }

  private def colored(color: String, text: String): String = if (colors) s"$color$text${LogFormat.Reset}" else text
}

/** The ANSI codes of [[dev.coilix.logging.LogFormat]]. */
object LogFormat {
  private val Reset      = "\u001b[0m"
  private val Green      = "\u001b[32m"
  private val Blue       = "\u001b[34m"
  private val Cyan       = "\u001b[36m"
  private val BoldRed    = "\u001b[1;31m"
  private val BoldYellow = "\u001b[1;33m"
}
