package dev.coilix.logging

import java.io.PrintStream

/**
 * Writes each event to `out`, normally the standard output, in [[dev.coilix.logging.LogFormat]]. Each entry, with its stack
 * trace, is written by one call, so entries of different threads do not interleave.
 */
case class ConsoleAppender(format: LogFormat, out: PrintStream) extends LogAppender {

  override def append(event: LogEvent): Unit = out.println(format.format(event))

  override def close(): Unit = out.flush()
}

/** Creation of the [[dev.coilix.logging.ConsoleAppender]]. */
object ConsoleAppender {

  /** An appender that writes to the standard output. */
  def standard(format: LogFormat): ConsoleAppender = ConsoleAppender(format, standardOutput)

  /** The standard output, which a console appender exists to write to. */
  @SuppressWarnings(Array("dev.coilix.warts.ConsoleOutput"))
  private def standardOutput: PrintStream = System.out
}
