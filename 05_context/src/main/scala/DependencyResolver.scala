package org.scaling.context

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait DependencyResolver {
  def resolve(definitions: List[BeanDefinition], provided: List[Class[?]]): List[BeanDefinition]
}

object DependencyResolver {
  given ConfigPath[DependencyResolver] = ConfigPath[DependencyResolver]("scaling.context.dependency-resolver")

  def default(using components: ComponentProvider): DependencyResolver =
    components.get[DependencyResolver](TopologicalDependencyResolver(CandidateSelector.default))
}
