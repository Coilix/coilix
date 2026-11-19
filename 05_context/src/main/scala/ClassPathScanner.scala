package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import java.lang.annotation.Annotation

/** Finds component classes in a package and creates their bean definitions. */
trait ClassPathScanner {

  /** Definitions for the components in `basePackage` and its subpackages. */
  def scan(basePackage: String): List[BeanDefinition]

  /**
   * The concrete classes in `basePackage` and its subpackages marked with any of `annotations`, sorted by their fully qualified
   * names and without bean definitions, for the annotations collected by modules.
   */
  def scanAnnotated(basePackage: String, annotations: List[Class[? <: Annotation]]): List[Class[?]]
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
