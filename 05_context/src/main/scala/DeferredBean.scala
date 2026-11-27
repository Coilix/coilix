package dev.coilix.context

import dev.coilix.common.GenericType
import java.util.concurrent.atomic.AtomicReference

/**
 * A bean whose instance is created when it is looked up: once for a [[dev.coilix.context.BeanCreation.Lazy]] singleton, which
 * then keeps it, and every time for a [[dev.coilix.context.BeanCreation.Prototype]]. Two threads that look up a lazy bean at the
 * same time get the same instance.
 *
 * @param beanName
 *   the name of the bean
 * @param beanType
 *   the type the bean is registered under
 * @param isPrimary
 *   whether the bean wins when several beans match a type
 * @param creation
 *   whether the instance is kept or created for every lookup
 * @param create
 *   creates the bean with dependencies looked up in the given registry
 * @param kept
 *   the instance of a lazy singleton once it is created
 */
case class DeferredBean(
  override val beanName:  String,
  override val beanType:  GenericType,
  override val isPrimary: Boolean,
  creation:               BeanCreation,
  create: BeanRegistry => RegisteredBean,
  kept:                   AtomicReference[Option[RegisteredBean]]
) extends BeanEntry {

  override def resolve(registry: BeanRegistry): RegisteredBean =
    creation match {
      case BeanCreation.Prototype => create(registry)
      case _                      => kept.get.getOrElse(createOnce(registry))
    }

  override def created: Option[RegisteredBean] = kept.get

  private def createOnce(registry: BeanRegistry): RegisteredBean =
    synchronized {
      kept.get.getOrElse(keep(create(registry)))
    }

  private def keep(bean: RegisteredBean): RegisteredBean = {
    kept.set(Some(bean))
    bean
  }
}

/** Factories of [[dev.coilix.context.DeferredBean]]. */
object DeferredBean {

  /** A deferred bean for `definition`, created by `create`. */
  def of(definition: BeanDefinition)(create: BeanRegistry => RegisteredBean): DeferredBean =
    DeferredBean(
      definition.beanName,
      definition.beanType,
      definition.isPrimary,
      definition.creation,
      create,
      AtomicReference(Option.empty[RegisteredBean])
    )
}
