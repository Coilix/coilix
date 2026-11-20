package dev.coilix.context

import dev.coilix.annotation.Slf4s
import dev.coilix.common.GenericType
import dev.coilix.config.Environment

/**
 * Creates beans through their [[dev.coilix.context.InstanceFactory]], resolving each dependency from the registry and `@Value`
 * parameters from the environment.
 *
 * Each new instance goes through the [[dev.coilix.context.BeanPostProcessor]] before it is returned, so `@PostConstruct`
 * methods have run before any other bean receives it. Each created bean is logged at `INFO` level. This is the default
 * [[dev.coilix.context.BeanInstantiator]].
 *
 * @param environment
 *   resolves `@Value` expressions
 * @param postProcessor
 *   initializes each new instance
 */
@Slf4s
final class ReflectiveBeanInstantiator(environment: Environment, postProcessor: BeanPostProcessor) extends BeanInstantiator {

  override def instantiate(definition: BeanDefinition, registry: BeanRegistry): RegisteredBean = {
    log.info(s"Creating bean ${definition.beanName} (${definition.beanClass.getSimpleName})")
    RegisteredBean(
      beanName  = definition.beanName,
      beanType  = definition.beanType,
      isPrimary = definition.isPrimary,
      instance  = postProcessor.postProcess(definition, newInstance(definition, registry)),
      lifecycle = definition.lifecycle
    )
  }

  private def newInstance(definition: BeanDefinition, registry: BeanRegistry): AnyRef =
    definition.factory.create(definition.dependencies.map(argument(definition, registry)))

  private def argument(definition: BeanDefinition, registry: BeanRegistry)(dependency: Dependency): AnyRef =
    dependency match {
      case Dependency.Required(beanType)           => required(definition, registry, beanType)
      case Dependency.Named(beanName, beanType)    => named(definition, registry, beanName, beanType)
      case Dependency.Optional(beanType)           => registry.find(beanType).map(_.instance)
      case Dependency.Collection(beanType)         => registry.findAll(beanType).map(_.instance)
      case Dependency.Value(expression, valueType) => environment.resolve(expression, valueType)
    }

  private def required(definition: BeanDefinition, registry: BeanRegistry, beanType: GenericType): AnyRef =
    registry.find(beanType).map(_.instance).getOrElse(throw unsatisfied(definition, beanType))

  private def named(definition: BeanDefinition, registry: BeanRegistry, beanName: String, beanType: GenericType): AnyRef =
    registry
      .findByName(beanName)
      .filter(bean => beanType.isAssignableFrom(bean.beanType))
      .map(_.instance)
      .getOrElse(throw new NoSuchBeanException(s"No bean named '$beanName' of type '$beanType' for '${definition.beanName}'"))

  private def unsatisfied(definition: BeanDefinition, beanType: GenericType): NoSuchBeanException =
    new NoSuchBeanException(s"Cannot satisfy dependency '$beanType' while creating '${definition.beanName}'")
}
