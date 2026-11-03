package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Orders bean definitions so that each bean is created after its dependencies. */
trait DependencyResolver {

  /**
   * `definitions` in creation order.
   *
   * @param provided
   *   types of beans that already exist, which no definition has to wait for
   * @throws dev.coilix.context.NoSuchBeanException
   *   if a required dependency matches neither a definition nor a provided type
   * @throws dev.coilix.context.CircularDependencyException
   *   if the definitions depend on each other in a cycle
   */
  def resolve(definitions: List[BeanDefinition], provided: List[Class[?]]): List[BeanDefinition]
}

/** Configuration path and default implementation of [[dev.coilix.context.DependencyResolver]]. */
object DependencyResolver {

  /**
   * Replace the component in the configuration under `coilix.context.dependency-resolver`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[DependencyResolver] = ConfigPath[DependencyResolver]("coilix.context.dependency-resolver")

  /**
   * The configured implementation, or [[dev.coilix.context.TopologicalDependencyResolver]] when
   * `coilix.context.dependency-resolver` is not set.
   */
  def default(using components: ComponentProvider): DependencyResolver =
    components.get[DependencyResolver](TopologicalDependencyResolver(CandidateSelector.default))
}
