package dev.coilix.logging

import cats.syntax.all.*
import dev.coilix.config.ConfigError
import dev.coilix.config.ConfigError.orThrow
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

  private type Read[A] = Either[ConfigError, A]

  /**
   * The settings under [[path]] in `environment`.
   *
   * @return
   *   the settings, or a [[dev.coilix.config.ConfigError]] when a value cannot be read or a level is not one of `trace`,
   *   `debug`, `info`, `warn`, `error` and `off`
   */
  def read(environment: Environment): Read[LoggingSettings] = {
    val defaults = LoggingSettings()
    (
      environment.lookup[String](s"$path.level").flatMap(_.traverse(levelNamed(s"$path.level", _))),
      levelsIn(environment),
      environment.lookup[Boolean](s"$path.colors"),
      environment.lookup[Boolean](s"$path.async"),
      environment.lookup[Int](s"$path.queue-size")
    ).mapN { (level, levels, colors, async, queueSize) =>
      LoggingSettings(
        level.getOrElse(defaults.level),
        levels,
        colors.getOrElse(defaults.colors),
        async.getOrElse(defaults.async),
        queueSize.getOrElse(defaults.queueSize)
      )
    }
  }

  /**
   * The settings under [[path]] in `environment`, as [[dev.coilix.logging.LoggingSettings.read]] gives them.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   when a value cannot be read or a level is unknown
   */
  def from(environment: Environment): LoggingSettings =
    read(environment).orThrow

  private def levelsIn(environment: Environment): Read[Map[String, LogLevel]] =
    environment.lookup[AnyRef](s"$path.levels").flatMap {
      case Some(section: JavaMap[?, ?]) => flattened(Nil, section).map(_.toMap)
      case _                            => Right(Map.empty)
    }

  private def flattened(prefix: List[String], section: JavaMap[?, ?]): Read[List[(String, LogLevel)]] =
    section.asScala.toList.flatTraverse((key, value) => entriesOf(prefix :+ key.toString, value))

  private def entriesOf(name: List[String], value: Any): Read[List[(String, LogLevel)]] =
    value match {
      case nested: JavaMap[?, ?] => flattened(name, nested)
      case level =>
        levelNamed(s"$path.levels.${name.mkString(".")}", level.toString).map(found => List(name.mkString(".") -> found))
    }

  private def levelNamed(key: String, name: String): Read[LogLevel] =
    LogLevel
      .parse(name)
      .toRight(ConfigError.InvalidValue(key, classOf[LogLevel], s"'$name' is not one of trace, debug, info, warn, error and off"))
}
