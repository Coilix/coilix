package org.scaling.slf4s

import dotty.tools.dotc.ast.untpd.MemberDef
import dotty.tools.dotc.core.Contexts.Context

trait Slf4sAnnotationDetector {
  def isAnnotated(definition: MemberDef)(using Context): Boolean
}
