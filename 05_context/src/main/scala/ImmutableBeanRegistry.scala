package dev.coilix.context

/**
 * A [[dev.coilix.context.BeanRegistry]] backed by an immutable list.
 *
 * @param beans
 *   the registered beans, in registration order
 * @param selector
 *   chooses a bean in [[dev.coilix.context.BeanRegistry.find]]
 */
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

/** Factories for [[dev.coilix.context.ImmutableBeanRegistry]]. */
object ImmutableBeanRegistry {

  /** A registry without beans. */
  def empty(selector: CandidateSelector): BeanRegistry = ImmutableBeanRegistry(Nil, selector)
}
