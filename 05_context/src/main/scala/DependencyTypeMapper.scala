package org.scaling.context

import java.lang.reflect.Type
import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath

trait DependencyTypeMapper {
  def map(parameterType: Type): Dependency
}

object DependencyTypeMapper {
  given ConfigPath[DependencyTypeMapper] = ConfigPath[DependencyTypeMapper]("scaling.context.dependency-type-mapper")

  def default(using components: ComponentProvider): DependencyTypeMapper =
    components.get[DependencyTypeMapper](GenericDependencyTypeMapper())
}
