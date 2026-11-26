package dev.coilix.boot

import dev.coilix.config.ComponentProvider
import dev.coilix.config.ConfigPath
import scala.reflect.ClassTag

/** Decides which packages are scanned for components. */
trait BasePackageResolver {

  /**
   * The packages to scan for application class `T`, or a [[dev.coilix.boot.StartupError.NotABootApplication]] when `T` is not a
   * Coilix application. Subpackages are scanned too.
   */
  def resolve[T: ClassTag]: Either[StartupError, List[String]]
}

/** Configuration path and default implementation of [[dev.coilix.boot.BasePackageResolver]]. */
object BasePackageResolver {

  /**
   * Replace the component in the configuration under `coilix.boot.base-package-resolver`, with a `class` key naming the
   * implementation.
   */
  given ConfigPath[BasePackageResolver] = ConfigPath[BasePackageResolver]("coilix.boot.base-package-resolver")

  /**
   * The configured implementation, or [[dev.coilix.boot.AnnotationBasePackageResolver]] when `coilix.boot.base-package-resolver` is
   * not set.
   */
  def default(using components: ComponentProvider): BasePackageResolver =
    components.get[BasePackageResolver](AnnotationBasePackageResolver())
}
