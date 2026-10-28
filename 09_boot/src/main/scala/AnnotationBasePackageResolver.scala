package org.scaling.boot

import org.scaling.annotation.ScalingBootApplication
import org.scaling.common.ClassTags.runtimeTypeOf
import scala.reflect.ClassTag

final class AnnotationBasePackageResolver extends BasePackageResolver {

  override def resolve[T: ClassTag]: List[String] =
    basePackages(runtimeTypeOf[T], annotationOf(runtimeTypeOf[T]))

  private def annotationOf(applicationClass: Class[?]): ScalingBootApplication =
    Option(applicationClass.getAnnotation(classOf[ScalingBootApplication]))
      .getOrElse(throw NotABootApplicationException(applicationClass))

  private def basePackages(applicationClass: Class[?], annotation: ScalingBootApplication): List[String] =
    annotation.scanBasePackages.toList match {
      case Nil      => List(applicationClass.getPackageName)
      case declared => declared
    }
}
