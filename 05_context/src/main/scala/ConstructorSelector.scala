package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import java.lang.reflect.Constructor

/** Chooses the constructor used to create a bean. */
trait ConstructorSelector {

  /**
   * The constructor of `beanClass` to call.
   *
   * @throws dev.coilix.context.NoPublicConstructorException
   *   if `beanClass` has no usable constructor
   */
  def select(beanClass: Class[?]): Constructor[?]
}

/** Configuration path and default implementation of [[dev.coilix.context.ConstructorSelector]]. */
object ConstructorSelector {

  /**
   * Replace the component in the configuration under `coilix.context.constructor-selector`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[ConstructorSelector] = ConfigPath[ConstructorSelector]("coilix.context.constructor-selector")

  /**
   * The configured implementation, or [[dev.coilix.context.AutowiredConstructorSelector]] when
   * `coilix.context.constructor-selector` is not set.
   */
  def default(using components: ComponentProvider): ConstructorSelector =
    components.get[ConstructorSelector](AutowiredConstructorSelector())
}
