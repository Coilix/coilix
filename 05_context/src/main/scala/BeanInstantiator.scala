package org.scaling.context

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait BeanInstantiator {
  def instantiate(definition: BeanDefinition, registry: BeanRegistry): RegisteredBean
}

object BeanInstantiator {
  given ConfigPath[BeanInstantiator] = ConfigPath[BeanInstantiator]("scaling.context.bean-instantiator")

  def default(using components: ComponentProvider): BeanInstantiator =
    components.get[BeanInstantiator](ReflectiveBeanInstantiator(components.environment))
}
