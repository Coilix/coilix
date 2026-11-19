package dev.coilix.context

import dev.coilix.common.GenericType

/** The name, type and precedence of a bean, shared by definitions and registered beans. */
trait BeanDescriptor {

  /** Unique name of the bean. */
  def beanName: String

  /** Type under which the bean is registered and injected, with its type arguments. */
  def beanType: GenericType

  /** Class of [[dev.coilix.context.BeanDescriptor.beanType]]. */
  final def beanClass: Class[?] = beanType.erasure

  /** Whether the bean wins when several beans match a requested type. */
  def isPrimary: Boolean
}
