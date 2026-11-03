package dev.coilix.context

trait BeanDescriptor {
  def beanName: String

  def beanClass: Class[?]

  def isPrimary: Boolean
}
