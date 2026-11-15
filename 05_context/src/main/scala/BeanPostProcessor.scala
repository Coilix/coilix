package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Works on a bean after it is created and before it is registered, so dependent beans only see the processed instance. */
trait BeanPostProcessor {

  /**
   * The instance to register for `definition`, usually `instance` itself after initialization.
   *
   * @throws dev.coilix.context.BeanCreationException
   *   if the bean cannot be initialized
   */
  def postProcess(definition: BeanDefinition, instance: AnyRef): AnyRef
}

/** Configuration path and default implementation of [[dev.coilix.context.BeanPostProcessor]]. */
object BeanPostProcessor {

  /**
   * Replace the component in the configuration under `coilix.context.bean-post-processor`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[BeanPostProcessor] = ConfigPath[BeanPostProcessor]("coilix.context.bean-post-processor")

  /**
   * The configured implementation, or [[dev.coilix.context.PostConstructProcessor]] when `coilix.context.bean-post-processor` is
   * not set.
   */
  def default(using components: ComponentProvider): BeanPostProcessor =
    components.get[BeanPostProcessor](PostConstructProcessor())
}
