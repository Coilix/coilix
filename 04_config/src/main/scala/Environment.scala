package dev.coilix.config

import dev.coilix.config.ConfigError.orThrow
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
 *
 * Each reading operation comes in two forms. [[dev.coilix.config.Environment.lookup]],
 * [[dev.coilix.config.Environment.binding]] and [[dev.coilix.config.Environment.evaluate]] return a
 * [[dev.coilix.config.ConfigError]], so the caller decides what to do with it. [[dev.coilix.config.Environment.get]],
 * [[dev.coilix.config.Environment.bind]] and [[dev.coilix.config.Environment.resolve]] throw its
 * [[dev.coilix.config.ConfigurationException]], which suits code that runs while beans are created.
 */
trait Environment {

  /** Whether the configuration has a value at `path`. */
  def contains(path: String): Boolean

  /**
   * The profiles listed under `coilix.profiles.active`, in the order they are listed, as read by [[dev.coilix.config.Profiles]].
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if the key holds a section instead of a list or a text
   */
  def activeProfiles: List[String]

  /** The value at `path` converted to `T`, `None` when the path is not set, or the error if it cannot be converted. */
  def lookup[T: ClassTag](path: String): Either[ConfigError, Option[T]]

  /**
   * Creates an object of `targetType` from the value at `path`, as described in [[dev.coilix.config.ReflectiveConfigBinder]].
   *
   * @return
   *   the object, or the error if the value is missing or cannot be converted
   */
  def binding(path: String, targetType: Type): Either[ConfigError, AnyRef]

  /**
   * Evaluates a `@Value` expression and converts the result to `targetType`.
   *
   * A placeholder is looked up in the configuration and falls back to its default. A literal is converted as it is.
   *
   * @return
   *   the value, or the error if a placeholder without a default is not set or the value cannot be converted
   */
  def evaluate(expression: ValueExpression, targetType: Type): Either[ConfigError, AnyRef]

  /**
   * The value at `path` converted to `T`, or `None` when the path is not set.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if the value cannot be converted to `T`
   */
  final def get[T: ClassTag](path: String): Option[T] =
    lookup[T](path).orThrow

  /**
   * Creates an object of `targetType` from the value at `path`, as described in [[dev.coilix.config.ReflectiveConfigBinder]].
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if the value is missing or cannot be converted
   */
  final def bind(path: String, targetType: Type): AnyRef =
    binding(path, targetType).orThrow

  /**
   * Evaluates a `@Value` expression and converts the result to `targetType`, as [[dev.coilix.config.Environment.evaluate]]
   * does.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if a placeholder without a default is not set, or the value cannot be converted
   */
  final def resolve(expression: ValueExpression, targetType: Type): AnyRef =
    evaluate(expression, targetType).orThrow
}

/** Factories for [[dev.coilix.config.Environment]]. */
object Environment {

  /** An environment backed by `tree` that converts values with [[dev.coilix.config.ConfigBinder.default]]. */
  def of(tree: ConfigTree): Environment = DefaultEnvironment(tree, ConfigBinder.default)

  /** An environment without any values. */
  def empty: Environment = of(ConfigTree.empty)
}
