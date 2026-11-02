package io.github.coilix.context

import io.github.coilix.annotation.Slf4s
import io.github.coilix.config.Environment

@Slf4s
final class ReflectiveBeanInstantiator(environment: Environment) extends BeanInstantiator {

  override def instantiate(definition: BeanDefinition, registry: BeanRegistry): RegisteredBean = {
    log.info(s"Creating bean ${definition.beanName} (${definition.beanClass.getSimpleName})")
    RegisteredBean(definition.beanName, definition.beanClass, definition.isPrimary, newInstance(definition, registry))
  }

  private def newInstance(definition: BeanDefinition, registry: BeanRegistry): AnyRef =
    definition.factory.create(definition.dependencies.map(argument(definition, registry)))

  private def argument(definition: BeanDefinition, registry: BeanRegistry)(dependency: Dependency): AnyRef =
    dependency match {
      case Dependency.Required(beanType)           => required(definition, registry, beanType)
      case Dependency.Optional(beanType)           => registry.find(beanType).map(_.instance)
      case Dependency.Collection(beanType)         => registry.findAll(beanType).map(_.instance)
      case Dependency.Value(expression, valueType) => environment.resolve(expression, valueType)
    }

  private def required(definition: BeanDefinition, registry: BeanRegistry, beanType: Class[?]): AnyRef =
    registry.find(beanType).map(_.instance).getOrElse(throw unsatisfied(definition, beanType))

  private def unsatisfied(definition: BeanDefinition, beanType: Class[?]): NoSuchBeanException =
    new NoSuchBeanException(s"Cannot satisfy dependency '${beanType.getName}' while creating '${definition.beanName}'")
}
