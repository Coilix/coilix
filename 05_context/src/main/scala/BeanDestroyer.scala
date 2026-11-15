package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Releases the beans of a context when it closes. */
trait BeanDestroyer {

  /** Destroys `beans`, given in creation order. Does not throw, so every bean gets the chance to release its resources. */
  def destroy(beans: List[RegisteredBean]): Unit
}

/** Configuration path and default implementation of [[dev.coilix.context.BeanDestroyer]]. */
object BeanDestroyer {

  /**
   * Replace the component in the configuration under `coilix.context.bean-destroyer`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[BeanDestroyer] = ConfigPath[BeanDestroyer]("coilix.context.bean-destroyer")

  /** The configured implementation, or [[dev.coilix.context.PreDestroyInvoker]] when `coilix.context.bean-destroyer` is not set. */
  def default(using components: ComponentProvider): BeanDestroyer =
    components.get[BeanDestroyer](PreDestroyInvoker())
}
