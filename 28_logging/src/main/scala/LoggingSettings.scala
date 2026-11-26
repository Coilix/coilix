package dev.coilix.logging

import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import java.util.Map as JavaMap
import scala.jdk.CollectionConverters.*

/**
 * Settings of the `logging` module under `coilix.logging`.
 *
 * ```yaml
 * coilix:
 *   logging:
 *     level: info
 *     levels:
 *       org.apache.kafka: warn
 *       com.example.noisy: off
 *       com.example.billing.Invoices: debug
 *     colors: true
 *     async: true
 *     queue-size: 8192
 * ```
 *
 * In HOCON, quote names that contain dots, such as `"org.apache.kafka" = warn`; unquoted names are read as nested sections,
 * which works too as long as a package and its subpackage are not both set.
 *
 * @param level
 *   the level of every logger that `levels` does not name
 * @param levels
 *   levels by package or logger name; a name applies to its subpackages and nested classes, and `off` silences them
 * @param colors
 *   whether the console marks levels and times with ANSI colors
 * @param async
 *   whether messages are written by a background thread, so logging does not wait for the console
 * @param queueSize
 *   how many messages wait for the background thread before logging waits for it
 */
final case class LoggingSettings(
  level:     LogLevel              = LogLevel.Info,
  levels:    Map[String, LogLevel] = Map.empty,
  colors:    Boolean               = true,
  async:     Boolean               = false,
  queueSize: Int                   = 8192
) {

  /** The resolver of logger levels for these settings. */
  def resolver: LevelResolver = LevelResolver(level, levels)
}

/** Reading of [[dev.coilix.logging.LoggingSettings]] from the configuration. */
object LoggingSettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.logging"

  /**
   * The settings under [[path]] in `environment`.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   when a level is not one of `trace`, `debug`, `info`, `warn`, `error` and `off`
   */
  def from(environment: Environment): LoggingSettings = {
    val defaults     = LoggingSettings()
    LoggingSettings(
      level     = environment.get[String](s"$path.level").map(levelNamed(s"$path.level", _)).getOrElse(defaults.level),
      levels    = levelsIn(environment),
      colors    = environment.get[Boolean](s"$path.colors").getOrElse(defaults.colors),
      async     = environment.get[Boolean](s"$path.async").getOrElse(defaults.async),
      queueSize = environment.get[Int](s"$path.queue-size").getOrElse(defaults.queueSize)
    )
  }

  private def levelsIn(environment: Environment): Map[String, LogLevel] =
    environment.get[AnyRef](s"$path.levels") match {
      case Some(section: JavaMap[?, ?]) => flattened(Nil, section).toMap
      case _                            => Map.empty
    }

  private def flattened(prefix: List[String], section: JavaMap[?, ?]): List[(String, LogLevel)] =
    section.asScala.toList.flatMap((key, value) => entriesOf(prefix :+ key.toString, value))

  private def entriesOf(name: List[String], value: Any): List[(String, LogLevel)] =
    value match {
      case nested: JavaMap[?, ?] => flattened(name, nested)
      case level => List(name.mkString(".") -> levelNamed(s"$path.levels.${name.mkString(".")}", level.toString))
    }

  private def levelNamed(key: String, name: String): LogLevel =
    LogLevel
      .parse(name)
      .getOrElse(
        throw ConfigurationException(s"$key is '$name', but a level is one of trace, debug, info, warn, error and off")
      )
}
