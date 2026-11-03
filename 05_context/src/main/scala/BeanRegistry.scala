package dev.coilix.context

trait BeanRegistry {
  def beans: List[RegisteredBean]

  def register(bean: RegisteredBean): BeanRegistry

  def find(beanType: Class[?]): Option[RegisteredBean]

  def findAll(beanType: Class[?]): List[RegisteredBean]

  def findByName(beanName: String): Option[RegisteredBean]
}
