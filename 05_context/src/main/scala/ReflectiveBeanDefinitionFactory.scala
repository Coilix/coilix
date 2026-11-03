package dev.coilix.context

import dev.coilix.annotation.ConfigurationProperties
import dev.coilix.annotation.Primary
import dev.coilix.config.Environment
import java.lang.reflect.Modifier

/**
 * Creates bean definitions through reflection.
 *
 * A `@ConfigurationProperties` class is bound from the configuration. Any other class is created through the constructor
 * chosen by the [[dev.coilix.context.ConstructorSelector]], with one dependency per parameter. Interfaces and abstract classes give
 * no
 * definition. The `@Bean` methods of the class are added by the [[dev.coilix.context.BeanMethodScanner]].
 *
 * This is the default [[dev.coilix.context.BeanDefinitionFactory]].
 *
 * @param constructorSelector
 *   chooses the constructor of a component
 * @param parameterMapper
 *   classifies constructor parameters
 * @param nameGenerator
 *   names the bean
 * @param methodScanner
 *   finds `@Bean` methods
 * @param environment
 *   binds `@ConfigurationProperties` classes
 */
final class ReflectiveBeanDefinitionFactory(
  constructorSelector: ConstructorSelector,
  parameterMapper:     ParameterDependencyMapper,
  nameGenerator:       BeanNameGenerator,
  methodScanner:       BeanMethodScanner,
  environment:         Environment
) extends BeanDefinitionFactory {

  override def create(beanClass: Class[?]): List[BeanDefinition] =
    if (isInstantiable(beanClass)) classDefinition(beanClass) :: methodScanner.scan(beanClass)
    else Nil

  private def isInstantiable(beanClass: Class[?]): Boolean =
    !beanClass.isInterface && !Modifier.isAbstract(beanClass.getModifiers)

  private def classDefinition(beanClass: Class[?]): BeanDefinition =
    Option(beanClass.getAnnotation(classOf[ConfigurationProperties]))
      .map(properties => propertiesDefinition(beanClass, properties.value))
      .getOrElse(constructorDefinition(beanClass))

  private def constructorDefinition(beanClass: Class[?]): BeanDefinition = {
    val constructor = constructorSelector.select(beanClass)
    definitionOf(beanClass, ConstructorInstanceFactory(constructor), constructor.getParameters.toList.map(parameterMapper.map))
  }

  private def propertiesDefinition(beanClass: Class[?], prefix: String): BeanDefinition =
    definitionOf(beanClass, PropertiesInstanceFactory(environment, prefix, beanClass), Nil)

  private def definitionOf(beanClass: Class[?], factory: InstanceFactory, dependencies: List[Dependency]): BeanDefinition =
    BeanDefinition(
      beanName     = nameGenerator.generate(beanClass),
      beanClass    = beanClass,
      factory      = factory,
      dependencies = dependencies,
      isPrimary    = beanClass.isAnnotationPresent(classOf[Primary])
    )
}
