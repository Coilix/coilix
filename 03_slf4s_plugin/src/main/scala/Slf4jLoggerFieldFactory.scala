package dev.coilix.slf4s

import dotty.tools.dotc.ast.untpd.*
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Decorators.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.util.SourceFile

final class Slf4jLoggerFieldFactory extends LoggerFieldFactory {

  override def create(loggerName: String, source: SourceFile)(using Context): ValDef = {
    given SourceFile = source
    ValDef(LoggerField.name, InferredTypeTree(), getLogger(loggerName))
      .withMods(EmptyModifiers.withFlags(Flags.Private | Flags.Final))
  }

  private def getLogger(loggerName: String)(using Context, SourceFile): Tree =
    Apply(Select(loggerFactory, "getLogger".toTermName), List(Literal(Constant(loggerName))))

  private def loggerFactory(using Context, SourceFile): Tree =
    Select(Select(rootDot("org".toTermName), "slf4j".toTermName), "LoggerFactory".toTermName)
}
