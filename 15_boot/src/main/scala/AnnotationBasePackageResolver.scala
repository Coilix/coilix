package dev.coilix.boot

import dev.coilix.annotation.CoilixApplication
import dev.coilix.common.ClassTags.runtimeTypeOf
import scala.reflect.ClassTag

/**
 * Reads the packages to scan from `@CoilixApplication.scanBasePackages`, or uses the package of the application class when
 * the attribute is empty.
 *
 * Throws [[dev.coilix.boot.NotABootApplicationException]] when the class is not annotated with `@CoilixApplication`. This is the
 * default [[dev.coilix.boot.BasePackageResolver]].
 */
final class AnnotationBasePackageResolver extends BasePackageResolver {

  override def resolve[T: ClassTag]: List[String] =
    basePackages(runtimeTypeOf[T], annotationOf(runtimeTypeOf[T]))

  private def annotationOf(applicationClass: Class[?]): CoilixApplication =
    Option(applicationClass.getAnnotation(classOf[CoilixApplication]))
      .getOrElse(throw NotABootApplicationException(applicationClass))

  private def basePackages(applicationClass: Class[?], annotation: CoilixApplication): List[String] =
    annotation.scanBasePackages.toList match {
      case Nil      => List(applicationClass.getPackageName)
      case declared => declared
    }
}
