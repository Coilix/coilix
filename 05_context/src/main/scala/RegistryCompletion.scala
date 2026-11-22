package dev.coilix.context

import java.util.concurrent.atomic.AtomicReference

/**
 * Remembers the registry of a started context, shared by every registry built on the way to it, so a
 * [[dev.coilix.context.Provider]] created during the startup finds the beans created after its own bean.
 */
final class RegistryCompletion {

  private val completed = AtomicReference(Option.empty[BeanRegistry])

  /** Marks `registry` as the registry of the started context. */
  def complete(registry: BeanRegistry): Unit = completed.set(Some(registry))

  /** The registry of the started context, or `None` while it is still starting. */
  def registry: Option[BeanRegistry] = completed.get
}
