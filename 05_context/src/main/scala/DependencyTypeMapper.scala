package dev.coilix.context

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import java.lang.reflect.Type

trait DependencyTypeMapper {
  def map(parameterType: Type): Dependency
}

object DependencyTypeMapper {
  given ConfigPath[DependencyTypeMapper] = ConfigPath[DependencyTypeMapper]("coilix.context.dependency-type-mapper")

  def default(using components: ComponentProvider): DependencyTypeMapper =
    components.get[DependencyTypeMapper](GenericDependencyTypeMapper())
}
