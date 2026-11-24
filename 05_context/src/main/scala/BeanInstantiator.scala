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
   * @throws dev.coilix.context.BeanCreationException
   *   if the bean cannot be created or initialized
   */
  def instantiate(definition: BeanDefinition, registry: BeanRegistry): RegisteredBean =
    complete(definition, produce(definition, registry))

  /**
   * Calls the factory of `definition` with its dependencies from `registry` and returns what it returns: the instance of a
   * plain bean, or the effect of a bean whose [[dev.coilix.context.BeanDefinition.result]] is effectful.
   *
   * @throws dev.coilix.context.NoSuchBeanException
   *   if a required dependency is not in `registry`
   * @throws dev.coilix.context.BeanCreationException
   *   if the factory fails
   */
  def produce(definition: BeanDefinition, registry: BeanRegistry): AnyRef

  /**
   * Makes the bean of `definition` from its `instance`, the result of [[produce]] for a plain bean or the value of its effect
   * for an effectful one: finds its lifecycle methods and runs the post-processing, such as `@PostConstruct` methods.
   *
   * @throws dev.coilix.context.BeanCreationException
   *   if the bean cannot be initialized
   */
  def complete(definition: BeanDefinition, instance: AnyRef): RegisteredBean
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
    components.get[BeanInstantiator](
      ReflectiveBeanInstantiator(components.environment, BeanPostProcessor.default, LifecycleMethodScanner.default)
    )
}
