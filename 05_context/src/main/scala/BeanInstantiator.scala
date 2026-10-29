package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath

trait BeanInstantiator {
  def instantiate(definition: BeanDefinition, registry: BeanRegistry): RegisteredBean
}

object BeanInstantiator {
  given ConfigPath[BeanInstantiator] = ConfigPath[BeanInstantiator]("coilix.context.bean-instantiator")

  def default(using components: ComponentProvider): BeanInstantiator =
    components.get[BeanInstantiator](ReflectiveBeanInstantiator(components.environment))
}
