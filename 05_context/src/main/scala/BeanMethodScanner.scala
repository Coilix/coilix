package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Finds beans declared by methods of a class. */
trait BeanMethodScanner {

  /** Definitions for the bean methods of `owner`, or an empty list when it declares none. */
  def scan(owner: Class[?]): List[BeanDefinition]
}

/** Configuration path and default implementation of [[dev.coilix.context.BeanMethodScanner]]. */
object BeanMethodScanner {

  /**
   * Replace the component in the configuration under `coilix.context.bean-method-scanner`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[BeanMethodScanner] = ConfigPath[BeanMethodScanner]("coilix.context.bean-method-scanner")

  /**
   * The configured implementation, or [[dev.coilix.context.ReflectiveBeanMethodScanner]] when `coilix.context.bean-method-scanner`
   * is not set.
   */
  def default(using components: ComponentProvider): BeanMethodScanner =
    components.get[BeanMethodScanner](ReflectiveBeanMethodScanner(ParameterDependencyMapper.default))
}
