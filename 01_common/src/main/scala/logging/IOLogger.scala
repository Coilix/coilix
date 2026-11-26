package dev.coilix.common
package logging

import cats.effect.IO
import org.slf4j.Logger

/**
 * An SLF4J logger whose calls are `IO` values, so code written in `IO` logs without wrapping every call.
 *
 * `@Slf4s` adds it to an annotated class as the private field `logF`, next to the plain `log`:
 *
 * ```scala
 * @Slf4s
 * final class InvoiceExport(invoices: InvoiceRepository) {
 *   def export(id: InvoiceId): IO[Unit] =
 *     logF.info(s"Exporting $id") *> invoices.export(id).onError(error => logF.error(s"Export of $id failed", error))
 * }
 * ```
 *
 * The message is built only when the `IO` runs and the level is enabled, so a debug message costs nothing while debug is off.
 *
 * @param logger
 *   the logger that receives the messages
 */
case class IOLogger(logger: Logger) {

  /** Logs `message` at `TRACE` level. */
  def trace(message: => String): IO[Unit] = IO(if (logger.isTraceEnabled) logger.trace(message))

  /** Logs `message` with `error` at `TRACE` level. */
  def trace(message: => String, error: Throwable): IO[Unit] = IO(if (logger.isTraceEnabled) logger.trace(message, error))

  /** Logs `message` at `DEBUG` level. */
  def debug(message: => String): IO[Unit] = IO(if (logger.isDebugEnabled) logger.debug(message))

  /** Logs `message` with `error` at `DEBUG` level. */
  def debug(message: => String, error: Throwable): IO[Unit] = IO(if (logger.isDebugEnabled) logger.debug(message, error))

  /** Logs `message` at `INFO` level. */
  def info(message: => String): IO[Unit] = IO(if (logger.isInfoEnabled) logger.info(message))

  /** Logs `message` with `error` at `INFO` level. */
  def info(message: => String, error: Throwable): IO[Unit] = IO(if (logger.isInfoEnabled) logger.info(message, error))

  /** Logs `message` at `WARN` level. */
  def warn(message: => String): IO[Unit] = IO(if (logger.isWarnEnabled) logger.warn(message))

  /** Logs `message` with `error` at `WARN` level. */
  def warn(message: => String, error: Throwable): IO[Unit] = IO(if (logger.isWarnEnabled) logger.warn(message, error))

  /** Logs `message` at `ERROR` level. */
  def error(message: => String): IO[Unit] = IO(if (logger.isErrorEnabled) logger.error(message))

  /** Logs `message` with `error` at `ERROR` level. */
  def error(message: => String, error: Throwable): IO[Unit] = IO(if (logger.isErrorEnabled) logger.error(message, error))
}
