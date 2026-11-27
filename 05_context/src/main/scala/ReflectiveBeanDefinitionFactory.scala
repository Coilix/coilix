package dev.coilix.context

import dev.coilix.annotation.ConfigurationProperties
import dev.coilix.annotation.DependsOn
import dev.coilix.annotation.Lazy
import dev.coilix.annotation.Primary
import dev.coilix.annotation.Prototype
import dev.coilix.common.GenericType
import dev.coilix.config.Environment
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Modifier

/**
 * Creates bean definitions through reflection.
 *
 * A `@ConfigurationProperties` class is bound from the configuration. Any other class is created through the constructor
 * chosen by the [[dev.coilix.context.ConstructorSelector]], with one dependency per parameter. Interfaces and abstract classes give
 * no
 * definition. The `@Bean` methods of the class are added by the [[dev.coilix.context.BeanMethodScanner]]. Every definition gets the
 * lifecycle methods of its bean class from the [[dev.coilix.context.LifecycleMethodScanner]], and its
 * [[dev.coilix.context.BeanCreation]] from [[dev.coilix.annotation.Prototype]] or [[dev.coilix.annotation.Lazy]] on the class or
 * the `@Bean` method, and the names in [[dev.coilix.annotation.DependsOn]] on the class or the `@Bean` method.
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
 * @param lifecycleScanner
 *   finds `@PostConstruct` and `@PreDestroy` methods
 * @param conditionScanner
 *   reads the conditions such as `@Profile`
 */
case class ReflectiveBeanDefinitionFactory(
  constructorSelector: ConstructorSelector,
  parameterMapper:     ParameterDependencyMapper,
  nameGenerator:       BeanNameGenerator,
  methodScanner:       BeanMethodScanner,
  environment:         Environment,
  lifecycleScanner:    LifecycleMethodScanner,
  conditionScanner:    ConditionScanner
) extends BeanDefinitionFactory {

  override def create(beanClass: Class[?]): List[BeanDefinition] =
    if (isInstantiable(beanClass))
      (classDefinition(beanClass) :: methodScanner.scan(beanClass))
        .map(withLifecycle)
        .map(withConditions)
        .map(withCreation)
        .map(withDependsOn)
    else Nil

  private def withLifecycle(definition: BeanDefinition): BeanDefinition =
    definition.copy(lifecycle = lifecycleScanner.scan(definition.beanName, definition.beanClass))

  private def withConditions(definition: BeanDefinition): BeanDefinition =
    definition.copy(conditions = conditionScanner.scan(definition))

  private def withCreation(definition: BeanDefinition): BeanDefinition =
    definition.copy(creation = creationOf(annotatedElement(definition)))

  private def withDependsOn(definition: BeanDefinition): BeanDefinition =
    definition.copy(dependsOn = dependsOnOf(annotatedElement(definition)))

  private def dependsOnOf(element: AnnotatedElement): List[String] =
    Option(element.getAnnotation(classOf[DependsOn])).map(_.value.toList.distinct).getOrElse(Nil)

  private def annotatedElement(definition: BeanDefinition): AnnotatedElement =
    definition.factory match {
      case MethodInstanceFactory(method) => method
      case _                             => definition.beanClass
    }

  private def creationOf(element: AnnotatedElement): BeanCreation =
    if (element.isAnnotationPresent(classOf[Prototype])) BeanCreation.Prototype
    else if (element.isAnnotationPresent(classOf[Lazy])) BeanCreation.Lazy
    else BeanCreation.Eager

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
      beanType     = GenericType.of(beanClass),
      factory      = factory,
      dependencies = dependencies,
      isPrimary    = beanClass.isAnnotationPresent(classOf[Primary])
    )
}
