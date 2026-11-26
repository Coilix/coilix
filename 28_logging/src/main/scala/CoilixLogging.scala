package dev.coilix.logging

import dev.coilix.config.EnvironmentLoader
import org.slf4j.LoggerFactory
import scala.util.Try

/**
 * The logging of the process when the `logging` module is the SLF4J provider: one [[dev.coilix.logging.CoilixLoggerFactory]],
 * because SLF4J keeps one provider for the whole JVM.
 *
 * It starts with the settings of the configuration files, system properties and environment variables, read before any
 * context exists, and the [[dev.coilix.logging.LogLevels]] bean applies the settings of the context, with command-line options,
 * when the context starts.
 */
object CoilixLogging {

  /** The factory of every logger of the process. */
  val factory: CoilixLoggerFactory = CoilixLoggerFactory(startupSettings)

  /** Applies `settings` to every logger, also to the records of `java.util.logging`. */
  def configure(settings: LoggingSettings): Unit = {
    factory.configure(settings)
    JulBridge.follow(settings)
  }

  /** Whether SLF4J uses the loggers of this module, rather than another provider on the classpath. */
  def isActive: Boolean = LoggerFactory.getILoggerFactory eq factory

  private def startupSettings: LoggingSettings =
    Try(LoggingSettings.from(EnvironmentLoader.default.load(Map.empty))).getOrElse(LoggingSettings())
}
