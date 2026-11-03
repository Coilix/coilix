package dev.coilix.config

import java.lang.reflect.Type
import scala.reflect.ClassTag

/**
 * The application configuration after all sources are merged.
 *
 * Coilix registers the environment as a bean named `environment`, so components can inject it to read values that are not
 * known in advance. For fixed keys, `@Value` and `@ConfigurationProperties` are usually simpler.
 *
 * ```scala
 * @Component
 * final class FeatureFlags(environment: Environment) {
 *   def isEnabled(feature: String): Boolean =
 *     environment.get[Boolean](s"shop.features.$feature").getOrElse(false)
 * }
 * ```
 */
trait Environment {

  /** Whether the configuration has a value at `path`. */
  def contains(path: String): Boolean

  /**
   * The value at `path` converted to `T`, or `None` when the path is not set.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if the value cannot be converted to `T`
   */
  def get[T: ClassTag](path: String): Option[T]

  /**
   * Creates an object of `targetType` from the value at `path`, as described in [[dev.coilix.config.ReflectiveConfigBinder]].
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if the value is missing or cannot be converted
   */
  def bind(path: String, targetType: Type): AnyRef

  /**
   * Evaluates a `@Value` expression and converts the result to `targetType`.
   *
   * A placeholder is looked up in the configuration and falls back to its default. A literal is converted as it is.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if a placeholder without a default is not set, or the value cannot be converted
   */
  def resolve(expression: ValueExpression, targetType: Type): AnyRef
}

/** Factories for [[dev.coilix.config.Environment]]. */
object Environment {

  /** An environment backed by `tree` that converts values with [[dev.coilix.config.ConfigBinder.default]]. */
  def of(tree: ConfigTree): Environment = DefaultEnvironment(tree, ConfigBinder.default)

  /** An environment without any values. */
  def empty: Environment = of(ConfigTree.empty)
}
