package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import scala.reflect.ClassTag

trait BasePackageResolver {
  def resolve[T: ClassTag]: List[String]
}

object BasePackageResolver {
  given ConfigPath[BasePackageResolver] = ConfigPath[BasePackageResolver]("coilix.boot.base-package-resolver")

  def default(using components: ComponentProvider): BasePackageResolver =
    components.get[BasePackageResolver](AnnotationBasePackageResolver())
}
