package dev.coilix.logging

import cats.effect.IO

/**
 * The [[dev.coilix.logging.LogLevels]] of the `logging` module, changing the loggers of `factory`, the `logLevels` bean. In an
 * application it changes [[dev.coilix.logging.CoilixLogging.factory]], and `java.util.logging` follows it.
 */
case class FactoryLogLevels(factory: CoilixLoggerFactory, onChange: LoggingSettings => Unit) extends LogLevels {

  override def root: IO[LogLevel] = IO(factory.current.level)

  override def levels: IO[Map[String, LogLevel]] = IO(factory.current.levels)

  override def levelOf(name: String): IO[LogLevel] = IO(factory.current.resolver.levelOf(name))

  override def set(name: String, level: LogLevel): IO[Unit] = changed(factory.setLevel(name, Some(level)))

  override def reset(name: String): IO[Unit] = changed(factory.setLevel(name, None))

  override def setRoot(level: LogLevel): IO[Unit] = changed(factory.setRootLevel(level))

  private def changed(change: => Unit): IO[Unit] = IO {
    change
    onChange(factory.current)
  }
}
