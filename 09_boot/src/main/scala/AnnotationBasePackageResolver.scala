package dev.coilix.boot

import dev.coilix.annotation.CoilixApplication
import dev.coilix.common.ClassTags.runtimeTypeOf
import scala.reflect.ClassTag

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
