package dev.coilix.intellij

import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector

final class Slf4sMembersInjector extends SyntheticMembersInjector {
  private val policy = LoggerMemberPolicy.default

  override def injectFunctions(source: ScTypeDefinition): Seq[String] =
    policy.loggerMembers(annotationTypes(source), memberNames(source))

  private def annotationTypes(source: ScTypeDefinition): List[String] =
    source.annotations.map(_.typeElement.getText).toList

  private def memberNames(source: ScTypeDefinition): List[String] =
    (source.functions.map(_.name) ++ source.properties.flatMap(_.declaredNames)).toList
}
