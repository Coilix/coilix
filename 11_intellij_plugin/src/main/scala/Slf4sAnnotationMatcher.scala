package org.scaling.intellij

trait Slf4sAnnotationMatcher {
  def matches(annotationType: String): Boolean
}
