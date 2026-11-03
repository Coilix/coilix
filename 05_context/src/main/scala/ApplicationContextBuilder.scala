package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Creates the beans from their definitions and returns the context that holds them. */
trait ApplicationContextBuilder {

  /**
   * Creates every bean in `definitions` in dependency order.
   *
   * @param singletons
   *   ready instances added to the context before any bean is created, such as the environment
   * @throws dev.coilix.context.NoSuchBeanException
   *   if a required dependency has no matching bean
   * @throws dev.coilix.context.CircularDependencyException
   *   if beans depend on each other in a cycle
   */
  def build(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ConfigurableApplicationContext

  /** Creates every bean in `definitions`, without ready instances. */
  final def build(definitions: List[BeanDefinition]): ConfigurableApplicationContext = build(definitions, Nil)
}

/** Configuration path and default implementation of [[dev.coilix.context.ApplicationContextBuilder]]. */
object ApplicationContextBuilder {

  /** Replace the component in the configuration under `coilix.context.application-context-builder`. */
  given ConfigPath[ApplicationContextBuilder] = ConfigPath[ApplicationContextBuilder]("coilix.context.application-context-builder")

  /**
   * The configured implementation, or a [[dev.coilix.context.DefaultApplicationContextBuilder]] built from the default dependency
   * resolver,
   * bean instantiator and candidate selector.
   */
  def default(using components: ComponentProvider): ApplicationContextBuilder =
    components.get[ApplicationContextBuilder](
      DefaultApplicationContextBuilder(DependencyResolver.default, BeanInstantiator.default, CandidateSelector.default)
    )
}
