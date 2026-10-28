package org.scaling.context

import java.lang.reflect.Modifier
import org.scaling.annotation.ConfigurationProperties
import org.scaling.annotation.Primary
import org.scaling.config.Environment

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
