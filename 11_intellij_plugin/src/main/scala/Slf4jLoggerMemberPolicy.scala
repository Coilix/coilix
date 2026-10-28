package org.scaling.intellij

final class Slf4jLoggerMemberPolicy(matcher: Slf4sAnnotationMatcher) extends LoggerMemberPolicy {
  private val loggerName   = "log"
  private val loggerMember = "def log: _root_.org.slf4j.Logger = ???"

  override def loggerMembers(annotationTypes: List[String], memberNames: List[String]): List[String] =
    if (needsLogger(annotationTypes, memberNames)) List(loggerMember)
    else Nil

  private def needsLogger(annotationTypes: List[String], memberNames: List[String]): Boolean =
    annotationTypes.exists(matcher.matches) && !memberNames.contains(loggerName)
}
