package dev.coilix.config

import java.lang.reflect.Type

/** Creates objects of a requested type from a configuration tree. */
trait ConfigBinder {

  /**
   * Converts the value at `path` to `targetType`.
   *
   * `targetType` may be a parameterized type such as `Option[Int]` or `List[MailSettings]`, so that type arguments are known.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if the value is missing or cannot be converted
   */
  def bind(tree: ConfigTree, path: String, targetType: Type): AnyRef
}

/** Factories for [[dev.coilix.config.ConfigBinder]]. */
object ConfigBinder {

  /**
   * The binder used by Coilix: a [[dev.coilix.config.ReflectiveConfigBinder]] with the standard scalar types and Scala default
   * arguments, loading classes through the context class loader of the current thread.
   */
  def default: ConfigBinder =
    ReflectiveConfigBinder(DefaultScalarReader(), ScalaDefaultArguments(), Thread.currentThread().getContextClassLoader)
}
