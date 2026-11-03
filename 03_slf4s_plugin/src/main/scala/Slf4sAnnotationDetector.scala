package dev.coilix.slf4s

import dotty.tools.dotc.ast.untpd.MemberDef
import dotty.tools.dotc.core.Contexts.Context

/** Decides whether a class or object definition asks for a logger. */
trait Slf4sAnnotationDetector {

  /** Whether `definition` is annotated with `@Slf4s`. */
  def isAnnotated(definition: MemberDef)(using Context): Boolean
}
