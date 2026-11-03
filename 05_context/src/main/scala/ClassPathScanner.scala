package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath

/** Finds component classes in a package and creates their bean definitions. */
trait ClassPathScanner {

  /** Definitions for the components in `basePackage` and its subpackages. */
  def scan(basePackage: String): List[BeanDefinition]
}

/** Configuration path and default implementation of [[dev.coilix.context.ClassPathScanner]]. */
object ClassPathScanner {

  /**
   * Replace the component in the configuration under `coilix.context.class-path-scanner`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[ClassPathScanner] = ConfigPath[ClassPathScanner]("coilix.context.class-path-scanner")

  /**
   * The configured implementation, or [[dev.coilix.context.ReflectionsClassPathScanner]] when `coilix.context.class-path-scanner`
   * is not set.
   */
  def default(using components: ComponentProvider): ClassPathScanner =
    components.get[ClassPathScanner](ReflectionsClassPathScanner(BeanDefinitionFactory.default))
}
