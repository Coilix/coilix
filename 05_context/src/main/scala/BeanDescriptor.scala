package io.github.coilix.context

trait BeanDescriptor {
  def beanName: String

  def beanClass: Class[?]

  def isPrimary: Boolean
}
