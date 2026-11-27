package dev.coilix.common
package logging

import cats.effect.Sync
import org.slf4j.Logger

/**
 * An SLF4J logger whose calls are values of the effect `F`, for any `F` with `Sync`: `IO`, `Resource[IO, *]`,
 * `EitherT[IO, E, *]` or the `F` of polymorphic code.
 *
 * The `logF` field added by `@Slf4s` gives it as `logF[F]`:
 *
 * ```scala
 * def checked(id: InvoiceId): EitherT[IO, InvoiceError, Invoice] =
 *   for {
 *     _       <- logF[[A] =>> EitherT[IO, InvoiceError, A]].info(s"Checking $id")
 *     invoice <- invoices.find(id)
 *   } yield invoice
 * ```
 *
 * The message is built only when the effect runs and the level is enabled.
 *
 * @param logger
 *   the SLF4J logger that writes the entries
 * @tparam F
 *   the effect of the calls
 */
case class LoggerF[F[_]: Sync](logger: Logger) {

  /** Logs `message` at `TRACE` level. */
  def trace(message: => String): F[Unit] = Sync[F].delay(if (logger.isTraceEnabled) logger.trace(message))

  /** Logs `message` with `failure` at `TRACE` level. */
  def trace(message: => String, failure: Throwable): F[Unit] =
    Sync[F].delay(if (logger.isTraceEnabled) logger.trace(message, failure))

  /** Logs `message` at `DEBUG` level. */
  def debug(message: => String): F[Unit] = Sync[F].delay(if (logger.isDebugEnabled) logger.debug(message))

  /** Logs `message` with `failure` at `DEBUG` level. */
  def debug(message: => String, failure: Throwable): F[Unit] =
    Sync[F].delay(if (logger.isDebugEnabled) logger.debug(message, failure))

  /** Logs `message` at `INFO` level. */
  def info(message: => String): F[Unit] = Sync[F].delay(if (logger.isInfoEnabled) logger.info(message))

  /** Logs `message` with `failure` at `INFO` level. */
  def info(message: => String, failure: Throwable): F[Unit] =
    Sync[F].delay(if (logger.isInfoEnabled) logger.info(message, failure))

  /** Logs `message` at `WARN` level. */
  def warn(message: => String): F[Unit] = Sync[F].delay(if (logger.isWarnEnabled) logger.warn(message))

  /** Logs `message` with `failure` at `WARN` level. */
  def warn(message: => String, failure: Throwable): F[Unit] =
    Sync[F].delay(if (logger.isWarnEnabled) logger.warn(message, failure))

  /** Logs `message` at `ERROR` level. */
  def error(message: => String): F[Unit] = Sync[F].delay(if (logger.isErrorEnabled) logger.error(message))

  /** Logs `message` with `failure` at `ERROR` level. */
  def error(message: => String, failure: Throwable): F[Unit] =
    Sync[F].delay(if (logger.isErrorEnabled) logger.error(message, failure))
}
