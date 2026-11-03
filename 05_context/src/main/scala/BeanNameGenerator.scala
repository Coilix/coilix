package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Chooses the name of a bean created from a class. */
trait BeanNameGenerator {

  /** The bean name for `beanClass`. */
  def generate(beanClass: Class[?]): String
}

/** Configuration path and default implementation of [[dev.coilix.context.BeanNameGenerator]]. */
object BeanNameGenerator {

  /**
   * Replace the component in the configuration under `coilix.context.bean-name-generator`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[BeanNameGenerator] = ConfigPath[BeanNameGenerator]("coilix.context.bean-name-generator")

  /**
   * The configured implementation, or [[dev.coilix.context.SimpleBeanNameGenerator]] when `coilix.context.bean-name-generator` is
   * not set.
   */
  def default(using components: ComponentProvider): BeanNameGenerator =
    components.get[BeanNameGenerator](SimpleBeanNameGenerator())
}
