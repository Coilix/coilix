package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Leaves out the bean definitions whose conditions do not hold, before their dependencies are resolved. */
trait DefinitionFilter {

  /**
   * The definitions that are added to the context.
   *
   * @param definitions
   *   all definitions, in the order of scanning
   * @param existing
   *   the classes of beans that are already registered, such as the ready singletons
   */
  def filter(definitions: List[BeanDefinition], existing: List[Class[?]]): List[BeanDefinition]
}

/** Configuration path and default implementation of [[dev.coilix.context.DefinitionFilter]]. */
object DefinitionFilter {

  /** Replace the component in the configuration under `coilix.context.definition-filter`, with a `class` key naming the implementation. */
  given ConfigPath[DefinitionFilter] = ConfigPath[DefinitionFilter]("coilix.context.definition-filter")

  /**
   * The configured implementation, or a [[dev.coilix.context.ConditionalDefinitionFilter]] that checks the conditions against
   * the application configuration when `coilix.context.definition-filter` is not set.
   */
  def default(using components: ComponentProvider): DefinitionFilter =
    components.get[DefinitionFilter](ConditionalDefinitionFilter(components.environment))
}
