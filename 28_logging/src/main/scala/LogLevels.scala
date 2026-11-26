package dev.coilix.logging

import cats.effect.IO

/**
 * Reads and changes the levels of loggers while the application runs. Changes take effect at once and last until the next
 * change or the end of the process; the configuration keeps the levels the application starts with.
 *
 * ```scala
 * @Component
 * final class Troubleshooting(levels: LogLevels) {
 *   def traceBilling: IO[Unit] = levels.set("com.example.billing", LogLevel.Debug)
 *
 *   def silenceKafka: IO[Unit] = levels.set("org.apache.kafka", LogLevel.Off)
 * }
 * ```
 */
trait LogLevels {

  /** The level of loggers that have no level of their own. */
  def root: IO[LogLevel]

  /** The levels set for packages and loggers, by name. */
  def levels: IO[Map[String, LogLevel]]

  /** The level that the logger named `name` uses now. */
  def levelOf(name: String): IO[LogLevel]

  /** Sets the level of the package or logger `name` and of everything below it; `LogLevel.Off` silences them. */
  def set(name: String, level: LogLevel): IO[Unit]

  /** Removes the level of `name`, so it follows the closest package with a level, or the root. */
  def reset(name: String): IO[Unit]

  /** Sets the level of loggers that have no level of their own. */
  def setRoot(level: LogLevel): IO[Unit]
}
