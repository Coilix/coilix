package dev.coilix.context

/**
 * Describes how to create one bean: its name, type, factory and dependencies.
 *
 * Definitions are created by a [[dev.coilix.context.BeanDefinitionFactory]] during component scanning and turned into beans by an
 * [[dev.coilix.context.ApplicationContextBuilder]].
 *
 * @param beanName
 *   unique name of the bean
 * @param beanClass
 *   type under which the bean is registered and injected
 * @param factory
 *   creates the instance from the resolved dependencies
 * @param dependencies
 *   arguments passed to `factory`, in order
 * @param isPrimary
 *   whether the bean wins when several beans match a requested type
 * @param lifecycle
 *   methods invoked after creation and when the context closes
 */
final case class BeanDefinition(
  beanName:     String,
  beanClass:    Class[?],
  factory:      InstanceFactory,
  dependencies: List[Dependency],
  isPrimary:    Boolean,
  lifecycle:    LifecycleMethods = LifecycleMethods.none
) extends BeanDescriptor
