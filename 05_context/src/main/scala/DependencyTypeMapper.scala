package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import java.lang.reflect.Type

/** Classifies a parameter type as a required, optional or collection dependency. */
trait DependencyTypeMapper {

  /** The dependency for `parameterType`, including its type arguments. */
  def map(parameterType: Type): Dependency
}

/** Configuration path and default implementation of [[dev.coilix.context.DependencyTypeMapper]]. */
object DependencyTypeMapper {

  /**
   * Replace the component in the configuration under `coilix.context.dependency-type-mapper`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[DependencyTypeMapper] = ConfigPath[DependencyTypeMapper]("coilix.context.dependency-type-mapper")

  /**
   * The configured implementation, or [[dev.coilix.context.GenericDependencyTypeMapper]] when
   * `coilix.context.dependency-type-mapper` is not set.
   */
  def default(using components: ComponentProvider): DependencyTypeMapper =
    components.get[DependencyTypeMapper](GenericDependencyTypeMapper())
}
