package org.scaling.context

final case class RegisteredBean(
  beanName:  String,
  beanClass: Class[?],
  isPrimary: Boolean,
  instance:  AnyRef
) extends BeanDescriptor
