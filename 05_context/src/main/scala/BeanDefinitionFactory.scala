package org.scaling.context

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait BeanDefinitionFactory {
  def create(beanClass: Class[?]): List[BeanDefinition]
}

object BeanDefinitionFactory {
  given ConfigPath[BeanDefinitionFactory] = ConfigPath[BeanDefinitionFactory]("scaling.context.bean-definition-factory")

  def default(using components: ComponentProvider): BeanDefinitionFactory =
    components.get[BeanDefinitionFactory](
      ReflectiveBeanDefinitionFactory(
        ConstructorSelector.default,
        ParameterDependencyMapper.default,
        BeanNameGenerator.default,
        BeanMethodScanner.default,
        components.environment
      )
    )
}
