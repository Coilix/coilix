package dev.coilix.context

final case class BeanDefinition(
  beanName:     String,
  beanClass:    Class[?],
  factory:      InstanceFactory,
  dependencies: List[Dependency],
  isPrimary:    Boolean
) extends BeanDescriptor
