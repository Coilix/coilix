package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Creates bean definitions for a class found during component scanning. */
trait BeanDefinitionFactory {

  /**
   * Definitions for `beanClass` and for the beans it declares, such as `@Bean` methods of a `@Configuration` class.
   * Returns an empty list for classes that cannot be instantiated.
   */
  def create(beanClass: Class[?]): List[BeanDefinition]
}

/** Configuration path and default implementation of [[dev.coilix.context.BeanDefinitionFactory]]. */
object BeanDefinitionFactory {

  /**
   * Replace the component in the configuration under `coilix.context.bean-definition-factory`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[BeanDefinitionFactory] = ConfigPath[BeanDefinitionFactory]("coilix.context.bean-definition-factory")

  /**
   * The configured implementation, or [[dev.coilix.context.ReflectiveBeanDefinitionFactory]] when
   * `coilix.context.bean-definition-factory` is not set.
   */
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
