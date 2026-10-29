package io.github.coilix.context

final class ImmutableBeanRegistry(override val beans: List[RegisteredBean], selector: CandidateSelector) extends BeanRegistry {

  override def register(bean: RegisteredBean): BeanRegistry =
    ImmutableBeanRegistry(beans :+ bean, selector)

  override def find(beanType: Class[?]): Option[RegisteredBean] =
    selector.select(beanType, beans)

  override def findAll(beanType: Class[?]): List[RegisteredBean] =
    beans.filter(bean => beanType.isAssignableFrom(bean.beanClass))

  override def findByName(beanName: String): Option[RegisteredBean] =
    beans.find(_.beanName == beanName)
}

object ImmutableBeanRegistry {
  def empty(selector: CandidateSelector): BeanRegistry = ImmutableBeanRegistry(Nil, selector)
}
