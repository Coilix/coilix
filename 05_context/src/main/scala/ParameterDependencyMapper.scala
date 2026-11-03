package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import java.lang.reflect.Parameter

/** Classifies a constructor or `@Bean` method parameter as a dependency. */
trait ParameterDependencyMapper {

  /** The dependency that provides the value of `parameter`. */
  def map(parameter: Parameter): Dependency
}

/** Configuration path and default implementation of [[dev.coilix.context.ParameterDependencyMapper]]. */
object ParameterDependencyMapper {

  /**
   * Replace the component in the configuration under `coilix.context.parameter-dependency-mapper`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[ParameterDependencyMapper] = ConfigPath[ParameterDependencyMapper]("coilix.context.parameter-dependency-mapper")

  /**
   * The configured implementation, or [[dev.coilix.context.AnnotatedParameterDependencyMapper]] when
   * `coilix.context.parameter-dependency-mapper` is not set.
   */
  def default(using components: ComponentProvider): ParameterDependencyMapper =
    components.get[ParameterDependencyMapper](AnnotatedParameterDependencyMapper(DependencyTypeMapper.default))
}
