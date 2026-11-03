package dev.coilix.slf4s

import dotty.tools.dotc.ast.untpd.ValDef
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.util.SourceFile

/** Creates the definition of the logger field added to an annotated class or object. */
trait LoggerFieldFactory {

  /**
   * An untyped definition of the field [[dev.coilix.slf4s.LoggerField.name]].
   *
   * @param loggerName
   *   name of the logger, normally the fully qualified name of the class
   * @param source
   *   source file the definition belongs to, used for positions in compiler messages
   */
  def create(loggerName: String, source: SourceFile)(using Context): ValDef
}
