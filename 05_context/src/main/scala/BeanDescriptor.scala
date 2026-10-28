package org.scaling.context

trait BeanDescriptor {
  def beanName: String

  def beanClass: Class[?]

  def isPrimary: Boolean
}
