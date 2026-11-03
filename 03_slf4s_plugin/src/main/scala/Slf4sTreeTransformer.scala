package dev.coilix.slf4s

import dotty.tools.dotc.ast.untpd.Tree
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.util.SourceFile

/** Rewrites the untyped syntax tree of one source file. */
trait Slf4sTreeTransformer {

  /**
   * The rewritten tree.
   *
   * @param source
   *   the file the tree comes from
   */
  def transform(tree: Tree, source: SourceFile)(using Context): Tree
}

/** Factories for [[dev.coilix.slf4s.Slf4sTreeTransformer]]. */
object Slf4sTreeTransformer {

  /**
   * The transformer used by the plugin: a [[dev.coilix.slf4s.LoggerInjectingTreeTransformer]] that detects the annotation by name
   * and
   * creates SLF4J loggers.
   */
  def default: Slf4sTreeTransformer =
    LoggerInjectingTreeTransformer(NameBasedSlf4sAnnotationDetector(), Slf4jLoggerFieldFactory())
}
