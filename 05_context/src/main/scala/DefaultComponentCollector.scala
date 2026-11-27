package dev.coilix.context

import dev.coilix.annotation.Slf4s
import dev.coilix.common.GenericType

/**
 * Creates the definitions of the collected classes, leaves out those whose conditions do not hold, and returns a definition of
 * `collectedComponents` that depends on all their dependencies.
 *
 * Only the definition of each class itself is used, so `@Bean` methods of a collected class add no beans. Conditions that
 * depend on other beans, such as `@ConditionalOnMissingBean`, are checked against the other collected classes only. Each kept
 * class is logged at `INFO` level. This is the default [[dev.coilix.context.ComponentCollector]], selected when
 * `coilix.context.component-collector` is not set.
 *
 * @param factory
 *   creates the definition of each collected class
 * @param filter
 *   leaves out the classes whose conditions do not hold
 * @param postProcessor
 *   initializes each created instance
 * @param destroyer
 *   runs the `@PreDestroy` methods when an instance is closed
 */
@Slf4s
case class DefaultComponentCollector(
  factory:       BeanDefinitionFactory,
  filter:        DefinitionFilter,
  postProcessor: BeanPostProcessor,
  destroyer:     BeanDestroyer
) extends ComponentCollector {

  private val beanName = "collectedComponents"

  override def collect(classes: List[Class[?]]): BeanDefinition = {
    val definitions  = filter.filter(classes.flatMap(definitionOf), Nil)
    val dependencies = definitions.flatMap(_.dependencies).distinct
    definitions.foreach(definition => log.info(s"Collected component ${definition.beanClass.getName}"))
    BeanDefinition(
      beanName,
      GenericType.of(classOf[CollectedComponents]),
      holder(definitions, dependencies),
      dependencies,
      isPrimary = false
    )
  }

  private def definitionOf(componentClass: Class[?]): List[BeanDefinition] =
    factory.create(componentClass).filter(_.beanClass == componentClass).take(1)

  private def holder(definitions: List[BeanDefinition], dependencies: List[Dependency]): InstanceFactory =
    arguments => DefaultCollectedComponents(definitions, dependencies.zip(arguments).toMap, postProcessor, destroyer)
}
