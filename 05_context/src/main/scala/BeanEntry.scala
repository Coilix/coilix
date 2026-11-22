package dev.coilix.context

/**
 * A bean of a [[dev.coilix.context.BeanRegistry]]: either a [[dev.coilix.context.RegisteredBean]] with its instance or a
 * [[dev.coilix.context.DeferredBean]] whose instance is created when it is looked up.
 */
trait BeanEntry extends BeanDescriptor {

  /** The bean with its instance, created now if needed, with dependencies looked up in `registry`. */
  def resolve(registry: BeanRegistry): RegisteredBean

  /** The bean with its instance when the context holds one, `None` for a lazy bean not created yet and for a prototype. */
  def created: Option[RegisteredBean]
}
