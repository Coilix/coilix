package dev.coilix.config

import dev.coilix.common.ClassTags.runtimeTypeOf
import scala.reflect.ClassTag

/**
 * The configuration path at which a replaceable component of type `T` can be configured.
 *
 * Each replaceable framework component declares one as a `given` in its companion object, and
 * [[dev.coilix.config.ComponentProvider]] uses it to find the configured implementation.
 *
 * ```scala
 * object StartupLogger {
 *   given ConfigPath[StartupLogger] = ConfigPath[StartupLogger]("coilix.boot.startup-logger")
 * }
 * ```
 *
 * @param path
 *   dotted path of the component section, for example `coilix.boot.startup-logger`
 * @param componentType
 *   the component type, which every configured implementation must extend
 */
final case class ConfigPath[T](path: String, componentType: Class[T])

/** Factories for [[dev.coilix.config.ConfigPath]]. */
object ConfigPath {

  /** A path for component type `T`, taken from the `ClassTag` in scope. */
  def apply[T: ClassTag](path: String): ConfigPath[T] = ConfigPath(path, runtimeTypeOf[T])
}
