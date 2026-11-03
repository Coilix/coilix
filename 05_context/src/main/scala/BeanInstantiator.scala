package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Creates the instance of one bean from its definition. */
trait BeanInstantiator {

  /**
   * Creates the bean described by `definition`, taking its dependencies from `registry`.
   *
   * @throws dev.coilix.context.NoSuchBeanException
   *   if a required dependency is not in `registry`
   */
  def instantiate(definition: BeanDefinition, registry: BeanRegistry): RegisteredBean
}

/** Configuration path and default implementation of [[dev.coilix.context.BeanInstantiator]]. */
object BeanInstantiator {

  /**
   * Replace the component in the configuration under `coilix.context.bean-instantiator`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[BeanInstantiator] = ConfigPath[BeanInstantiator]("coilix.context.bean-instantiator")

  /**
   * The configured implementation, or [[dev.coilix.context.ReflectiveBeanInstantiator]] when `coilix.context.bean-instantiator` is
   * not set.
   */
  def default(using components: ComponentProvider): BeanInstantiator =
    components.get[BeanInstantiator](ReflectiveBeanInstantiator(components.environment))
}
