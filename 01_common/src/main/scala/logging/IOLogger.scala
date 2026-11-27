package dev.coilix.common
package logging

import cats.effect.IO
import cats.effect.Sync
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
 * The message is built only when the effect runs and the level is enabled, so a debug message costs nothing while debug is off.
 *
 * The same calls in another effect with `Sync`, such as `Resource[IO, *]`, `EitherT[IO, E, *]` or the `F` of polymorphic code,
 * come from `logF[F]`, a [[dev.coilix.common.logging.LoggerF]]:
 *
 * ```scala
 * def loaded[F[_]: Sync](id: InvoiceId): F[Invoice] =
 *   logF[F].debug(s"Loading $id") *> repository.load[F](id)
 * ```
 *
 * @param logger
 *   the logger that receives the messages
 */
case class IOLogger(logger: Logger) {

  private val io = LoggerF[IO](logger)

  /** The same logger with calls in the effect `F`. */
  def apply[F[_]: Sync]: LoggerF[F] = LoggerF[F](logger)

  /** Logs `message` at `TRACE` level. */
  def trace(message: => String): IO[Unit] = io.trace(message)

  /** Logs `message` with `failure` at `TRACE` level. */
  def trace(message: => String, failure: Throwable): IO[Unit] = io.trace(message, failure)

  /** Logs `message` at `DEBUG` level. */
  def debug(message: => String): IO[Unit] = io.debug(message)

  /** Logs `message` with `failure` at `DEBUG` level. */
  def debug(message: => String, failure: Throwable): IO[Unit] = io.debug(message, failure)

  /** Logs `message` at `INFO` level. */
  def info(message: => String): IO[Unit] = io.info(message)

  /** Logs `message` with `failure` at `INFO` level. */
  def info(message: => String, failure: Throwable): IO[Unit] = io.info(message, failure)

  /** Logs `message` at `WARN` level. */
  def warn(message: => String): IO[Unit] = io.warn(message)

  /** Logs `message` with `failure` at `WARN` level. */
  def warn(message: => String, failure: Throwable): IO[Unit] = io.warn(message, failure)

  /** Logs `message` at `ERROR` level. */
  def error(message: => String): IO[Unit] = io.error(message)

  /** Logs `message` with `failure` at `ERROR` level. */
  def error(message: => String, failure: Throwable): IO[Unit] = io.error(message, failure)
}
