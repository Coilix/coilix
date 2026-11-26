package dev.coilix.logging

import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.ILoggerFactory
import org.slf4j.Logger

/**
 * Creates the loggers of the `logging` module and changes their levels and their appender when the settings change.
 *
 * [[dev.coilix.logging.CoilixLoggerFactory.configure]] applies settings from the configuration, and
 * [[dev.coilix.logging.CoilixLoggerFactory.setLevel]] and [[dev.coilix.logging.CoilixLoggerFactory.setRootLevel]] change
 * single levels at runtime. Every change takes effect at once, also for loggers created before it.
 *
 * @param settings
 *   the current settings, including the levels changed at runtime
 * @param loggers
 *   the loggers created so far, by full name
 * @param appender
 *   the appender of every logger
 */
case class CoilixLoggerFactory(
  settings: AtomicReference[LoggingSettings],
  loggers:  AtomicReference[Map[String, CoilixLogger]],
  appender: AtomicReference[LogAppender]
) extends ILoggerFactory {

  override def getLogger(name: String): Logger =
    loggers.get.getOrElse(name, created(name))

  /** The current settings. */
  def current: LoggingSettings = settings.get

  /** Applies `next`: changes the level of every logger and, when the output changes, the appender. */
  def configure(next: LoggingSettings): Unit = {
    val previous = settings.getAndSet(next)
    if (outputChanged(previous, next)) replaceAppender(next)
    relevel()
  }

  /** Sets the level of the logger or package `name`, or removes its own level when `level` is `None`. */
  def setLevel(name: String, level: Option[LogLevel]): Unit = {
    settings.getAndUpdate(current => current.copy(levels = level.fold(current.levels - name)(current.levels.updated(name, _))))
    relevel()
  }

  /** Sets the level of every logger that has no level of its own. */
  def setRootLevel(level: LogLevel): Unit = {
    settings.getAndUpdate(_.copy(level = level))
    relevel()
  }

  /** Writes the waiting messages and closes the appender. */
  def close(): Unit = appender.get.close()

  private def created(name: String): CoilixLogger = {
    val logger = CoilixLogger(name, AtomicReference(settings.get.resolver.levelOf(name)), appender)
    loggers.updateAndGet(current => if (current.contains(name)) current else current.updated(name, logger))(name)
  }

  private def relevel(): Unit = {
    val resolver = settings.get.resolver
    loggers.get.values.foreach(logger => logger.level.set(resolver.levelOf(logger.loggerName)))
  }

  private def outputChanged(previous: LoggingSettings, next: LoggingSettings): Boolean =
    previous.colors != next.colors || previous.async != next.async || previous.queueSize != next.queueSize

  private def replaceAppender(next: LoggingSettings): Unit =
    appender.getAndSet(CoilixLoggerFactory.appenderFor(next)).close()
}

/** Creation of the [[dev.coilix.logging.CoilixLoggerFactory]]. */
object CoilixLoggerFactory {

  /** A factory with `settings` and its own appender. */
  def apply(settings: LoggingSettings): CoilixLoggerFactory =
    CoilixLoggerFactory(AtomicReference(settings), AtomicReference(Map.empty), AtomicReference(appenderFor(settings)))

  /** The appender for `settings`: the console, written by a background thread when `async` is true. */
  def appenderFor(settings: LoggingSettings): LogAppender = {
    val console = ConsoleAppender.standard(LogFormat(settings.colors, ZoneId.systemDefault))
    if (settings.async) AsyncAppender(console, settings.queueSize) else console
  }
}
