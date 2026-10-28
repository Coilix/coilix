package org.scaling.slf4s

import dotty.tools.dotc.ast.untpd.Tree
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.util.SourceFile

trait Slf4sTreeTransformer {
  def transform(tree: Tree, source: SourceFile)(using Context): Tree
}

object Slf4sTreeTransformer {
  def default: Slf4sTreeTransformer =
    LoggerInjectingTreeTransformer(NameBasedSlf4sAnnotationDetector(), Slf4jLoggerFieldFactory())
}
