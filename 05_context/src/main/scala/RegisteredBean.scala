package dev.coilix.context

/**
 * A created bean held by a [[dev.coilix.context.BeanRegistry]].
 *
 * @param beanName
 *   unique name of the bean
 * @param beanClass
 *   type under which the bean is registered and injected
 * @param isPrimary
 *   whether the bean wins when several beans match a requested type
 * @param instance
 *   the bean itself
 */
final case class RegisteredBean(
  beanName:  String,
  beanClass: Class[?],
  isPrimary: Boolean,
  instance:  AnyRef
) extends BeanDescriptor
