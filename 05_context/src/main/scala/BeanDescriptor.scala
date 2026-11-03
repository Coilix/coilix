package dev.coilix.context

/** The name, type and precedence of a bean, shared by definitions and registered beans. */
trait BeanDescriptor {

  /** Unique name of the bean. */
  def beanName: String

  /** Type under which the bean is registered and injected. */
  def beanClass: Class[?]

  /** Whether the bean wins when several beans match a requested type. */
  def isPrimary: Boolean
}
