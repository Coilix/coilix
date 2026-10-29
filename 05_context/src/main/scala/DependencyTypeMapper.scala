package io.github.coilix.context

import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.ConfigPath
import java.lang.reflect.Type

trait DependencyTypeMapper {
  def map(parameterType: Type): Dependency
}

object DependencyTypeMapper {
  given ConfigPath[DependencyTypeMapper] = ConfigPath[DependencyTypeMapper]("coilix.context.dependency-type-mapper")

  def default(using components: ComponentProvider): DependencyTypeMapper =
    components.get[DependencyTypeMapper](GenericDependencyTypeMapper())
}
