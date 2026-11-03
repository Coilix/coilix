package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

trait DependencyResolver {
  def resolve(definitions: List[BeanDefinition], provided: List[Class[?]]): List[BeanDefinition]
}

object DependencyResolver {
  given ConfigPath[DependencyResolver] = ConfigPath[DependencyResolver]("coilix.context.dependency-resolver")

  def default(using components: ComponentProvider): DependencyResolver =
    components.get[DependencyResolver](TopologicalDependencyResolver(CandidateSelector.default))
}
