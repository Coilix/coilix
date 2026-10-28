package org.scaling.context

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait ApplicationContextBuilder {

  def build(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ConfigurableApplicationContext

  final def build(definitions: List[BeanDefinition]): ConfigurableApplicationContext = build(definitions, Nil)
}

object ApplicationContextBuilder {
  given ConfigPath[ApplicationContextBuilder] = ConfigPath[ApplicationContextBuilder]("scaling.context.application-context-builder")

  def default(using components: ComponentProvider): ApplicationContextBuilder =
    components.get[ApplicationContextBuilder](
      DefaultApplicationContextBuilder(DependencyResolver.default, BeanInstantiator.default, CandidateSelector.default)
    )
}
