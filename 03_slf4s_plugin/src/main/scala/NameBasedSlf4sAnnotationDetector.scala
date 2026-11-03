package dev.coilix.slf4s

import dotty.tools.dotc.ast.untpd.*
import dotty.tools.dotc.core.Contexts.Context
import scala.annotation.tailrec

final class NameBasedSlf4sAnnotationDetector extends Slf4sAnnotationDetector {
  private val annotationName = "Slf4s"

  override def isAnnotated(definition: MemberDef)(using Context): Boolean =
    definition.mods.annotations.exists(refersToSlf4s)

  @tailrec
  private def refersToSlf4s(tree: Tree): Boolean =
    tree match {
      case apply:    Apply  => refersToSlf4s(apply.fun)
      case instance: New    => refersToSlf4s(instance.tpt)
      case select:   Select => select.name.toString == annotationName || refersToSlf4s(select.qualifier)
      case ident:    Ident  => ident.name.toString == annotationName
      case _ => false
    }
}
