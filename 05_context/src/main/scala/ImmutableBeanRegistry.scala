package dev.coilix.context

import dev.coilix.common.GenericType

/**
 * A [[dev.coilix.context.BeanRegistry]] backed by an immutable list.
 *
 * @param entries
 *   the beans, created and deferred, in registration order
 * @param selector
 *   chooses a bean in [[dev.coilix.context.BeanRegistry.find]]
 * @param completion
 *   gives the registry of the started context, shared by the registries built on the way to it
 */
case class ImmutableBeanRegistry(entries: List[BeanEntry], selector: CandidateSelector, completion: RegistryCompletion)
    extends BeanRegistry {

  override def beans: List[RegisteredBean] = entries.flatMap(_.created)

  override def beanNames: List[String] = entries.map(_.beanName)

  override def register(bean: RegisteredBean): BeanRegistry =
    ImmutableBeanRegistry(entries :+ bean, selector, completion)

  override def defer(bean: DeferredBean): BeanRegistry =
    ImmutableBeanRegistry(entries :+ bean, selector, completion)

  override def find(beanType: GenericType): Option[RegisteredBean] =
    selector.select(beanType, entries).map(_.resolve(this))

  override def findAll(beanType: GenericType): List[RegisteredBean] =
    entries.filter(entry => beanType.isAssignableFrom(entry.beanType)).map(_.resolve(this))

  override def findByName(beanName: String): Option[RegisteredBean] =
    entries.find(_.beanName == beanName).map(_.resolve(this))

  override def complete: BeanRegistry = completion.registry.getOrElse(this)
}

/** Factories for [[dev.coilix.context.ImmutableBeanRegistry]]. */
object ImmutableBeanRegistry {

  /** A registry without beans, whose [[dev.coilix.context.BeanRegistry.complete]] is given by `completion`. */
  def empty(selector: CandidateSelector, completion: RegistryCompletion = RegistryCompletion()): BeanRegistry =
    ImmutableBeanRegistry(Nil, selector, completion)
}
