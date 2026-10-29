package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath

trait BeanDefinitionFactory {
  def create(beanClass: Class[?]): List[BeanDefinition]
}

object BeanDefinitionFactory {
  given ConfigPath[BeanDefinitionFactory] = ConfigPath[BeanDefinitionFactory]("coilix.context.bean-definition-factory")

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
