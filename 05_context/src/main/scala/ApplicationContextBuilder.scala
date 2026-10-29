package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath

trait ApplicationContextBuilder {

  def build(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ConfigurableApplicationContext

  final def build(definitions: List[BeanDefinition]): ConfigurableApplicationContext = build(definitions, Nil)
}

object ApplicationContextBuilder {
  given ConfigPath[ApplicationContextBuilder] = ConfigPath[ApplicationContextBuilder]("coilix.context.application-context-builder")

  def default(using components: ComponentProvider): ApplicationContextBuilder =
    components.get[ApplicationContextBuilder](
      DefaultApplicationContextBuilder(DependencyResolver.default, BeanInstantiator.default, CandidateSelector.default)
    )
}
