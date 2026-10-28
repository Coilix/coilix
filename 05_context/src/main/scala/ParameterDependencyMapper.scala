package org.scaling.context

import java.lang.reflect.Parameter
import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait ParameterDependencyMapper {
  def map(parameter: Parameter): Dependency
}

object ParameterDependencyMapper {
  given ConfigPath[ParameterDependencyMapper] = ConfigPath[ParameterDependencyMapper]("scaling.context.parameter-dependency-mapper")

  def default(using components: ComponentProvider): ParameterDependencyMapper =
    components.get[ParameterDependencyMapper](AnnotatedParameterDependencyMapper(DependencyTypeMapper.default))
}
