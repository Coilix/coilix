package dev.coilix.intellij

trait LoggerMemberPolicy {
  def loggerMembers(annotationTypes: List[String], memberNames: List[String]): List[String]
}

object LoggerMemberPolicy {
  def default: LoggerMemberPolicy = new Slf4jLoggerMemberPolicy(new SimpleNameSlf4sAnnotationMatcher)
}
