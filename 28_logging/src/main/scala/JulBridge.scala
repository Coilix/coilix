package dev.coilix.logging

import java.util.logging.Level as JulLevel
import java.util.logging.LogManager
import org.slf4j.bridge.SLF4JBridgeHandler

/**
 * Sends `java.util.logging` to SLF4J, so libraries that log through it write in the format of the `logging` module.
 *
 * The level of the root logger of `java.util.logging` follows the lowest level of the settings, so it drops only the records
 * that no logger of the module would write.
 */
object JulBridge {

  /** Replaces the handlers of the root logger of `java.util.logging` with the bridge to SLF4J. */
  def install(): Unit = {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
  }

  /** Lets through the records of the lowest level that `settings` enable. */
  def follow(settings: LoggingSettings): Unit =
    LogManager.getLogManager.getLogger("").setLevel(julLevelOf(lowest(settings)))

  private def lowest(settings: LoggingSettings): LogLevel = (settings.level :: settings.levels.values.toList).minBy(_.severity)

  private def julLevelOf(level: LogLevel): JulLevel =
    level match {
      case LogLevel.Trace => JulLevel.FINEST
      case LogLevel.Debug => JulLevel.FINE
      case LogLevel.Info  => JulLevel.INFO
      case LogLevel.Warn  => JulLevel.WARNING
      case LogLevel.Error => JulLevel.SEVERE
      case LogLevel.Off   => JulLevel.OFF
    }
}
