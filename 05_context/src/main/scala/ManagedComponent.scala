package dev.coilix.context

import java.util.concurrent.atomic.AtomicBoolean

/**
 * An instance created by [[dev.coilix.context.CollectedComponents]], together with its `@PreDestroy` methods.
 *
 * The context does not destroy collected instances, so the module that created one closes it, usually from its own
 * `@PreDestroy` method.
 *
 * @param instance
 *   the created component
 * @param bean
 *   the instance with its lifecycle methods, passed to `destroyer`
 * @param destroyer
 *   runs the `@PreDestroy` methods
 */
case class ManagedComponent[T](instance: T, bean: RegisteredBean, destroyer: BeanDestroyer) extends AutoCloseable {

  private val open = AtomicBoolean(true)

  /** Whether `close` has not been called yet. */
  def isOpen: Boolean = open.get

  /** Runs the `@PreDestroy` methods of the instance. Closing an already closed component does nothing. */
  override def close(): Unit =
    if (open.compareAndSet(true, false)) destroyer.destroy(List(bean))
}
