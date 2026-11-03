package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import java.lang.reflect.Parameter

trait ParameterDependencyMapper {
  def map(parameter: Parameter): Dependency
}

object ParameterDependencyMapper {
  given ConfigPath[ParameterDependencyMapper] = ConfigPath[ParameterDependencyMapper]("coilix.context.parameter-dependency-mapper")

  def default(using components: ComponentProvider): ParameterDependencyMapper =
    components.get[ParameterDependencyMapper](AnnotatedParameterDependencyMapper(DependencyTypeMapper.default))
}
