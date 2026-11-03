package dev.coilix.slf4s

import dotty.tools.dotc.ast.untpd.*
import dotty.tools.dotc.core.Contexts.Context
import scala.annotation.tailrec

/**
 * Recognizes the annotation by its simple name `Slf4s`, written either as `@Slf4s` or with a qualified name such as
 * `@dev.coilix.annotation.Slf4s`.
 *
 * The plugin runs before type checking, so names are not resolved yet and any annotation named `Slf4s` matches. This is
 * the detector used by [[dev.coilix.slf4s.Slf4sTreeTransformer.default]].
 */
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
