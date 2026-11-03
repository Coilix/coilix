package dev.coilix.intellij

trait Slf4sAnnotationMatcher {
  def matches(annotationType: String): Boolean
}
