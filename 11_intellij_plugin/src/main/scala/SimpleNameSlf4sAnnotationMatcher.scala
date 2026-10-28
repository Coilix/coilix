package org.scaling.intellij

final class SimpleNameSlf4sAnnotationMatcher extends Slf4sAnnotationMatcher {
  private val annotationName = "Slf4s"

  override def matches(annotationType: String): Boolean =
    simpleName(annotationType) == annotationName

  private def simpleName(annotationType: String): String =
    annotationType.split('.').lastOption.fold("")(_.trim)
}
