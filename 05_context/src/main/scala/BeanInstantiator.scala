package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

trait BeanInstantiator {
  def instantiate(definition: BeanDefinition, registry: BeanRegistry): RegisteredBean
}

object BeanInstantiator {
  given ConfigPath[BeanInstantiator] = ConfigPath[BeanInstantiator]("coilix.context.bean-instantiator")

  def default(using components: ComponentProvider): BeanInstantiator =
    components.get[BeanInstantiator](ReflectiveBeanInstantiator(components.environment))
}
