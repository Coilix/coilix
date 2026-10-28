package org.scaling.boot

import org.scaling.config.ComponentProvider
import org.scaling.config.ConfigPath
import scala.reflect.ClassTag

trait BasePackageResolver {
  def resolve[T: ClassTag]: List[String]
}

object BasePackageResolver {
  given ConfigPath[BasePackageResolver] = ConfigPath[BasePackageResolver]("scaling.boot.base-package-resolver")

  def default(using components: ComponentProvider): BasePackageResolver =
    components.get[BasePackageResolver](AnnotationBasePackageResolver())
}
