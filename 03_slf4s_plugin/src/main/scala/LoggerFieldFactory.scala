package org.scaling.slf4s

import dotty.tools.dotc.ast.untpd.ValDef
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.util.SourceFile

trait LoggerFieldFactory {
  def create(loggerName: String, source: SourceFile)(using Context): ValDef
}
