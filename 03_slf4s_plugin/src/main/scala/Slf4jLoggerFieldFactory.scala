package dev.coilix.slf4s

import dotty.tools.dotc.ast.untpd.*
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Decorators.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Symbols.getClassIfDefined
import dotty.tools.dotc.util.SourceFile

/**
 * Creates `private final val log = _root_.org.slf4j.LoggerFactory.getLogger("<name>")`.
 *
 * The type of the field is inferred, so it is `org.slf4j.Logger`. SLF4J must be on the compile class path of the module.
 * This is the factory used by [[dev.coilix.slf4s.Slf4sTreeTransformer.default]].
 */
final class Slf4jLoggerFieldFactory extends LoggerFieldFactory {

  override def create(loggerName: String, source: SourceFile)(using Context): ValDef = {
    given SourceFile = source
    ValDef(LoggerField.name, InferredTypeTree(), getLogger(loggerName))
      .withMods(EmptyModifiers.withFlags(Flags.Private | Flags.Final))
  }

  override def createEffectful(source: SourceFile)(using Context): Option[ValDef] = {
    given SourceFile = source
    Option.when(getClassIfDefined(effectfulLoggerClass).exists) {
      ValDef(LoggerField.effectfulName, InferredTypeTree(), Apply(effectfulLogger, List(Ident(LoggerField.name))))
        .withMods(EmptyModifiers.withFlags(Flags.Private | Flags.Final))
    }
  }

  private val effectfulLoggerClass = "dev.coilix.common.logging.IOLogger"

  private def effectfulLogger(using Context, SourceFile): Tree =
    Select(
      Select(Select(Select(rootDot("dev".toTermName), "coilix".toTermName), "common".toTermName), "logging".toTermName),
      "IOLogger".toTermName
    )

  private def getLogger(loggerName: String)(using Context, SourceFile): Tree =
    Apply(Select(loggerFactory, "getLogger".toTermName), List(Literal(Constant(loggerName))))

  private def loggerFactory(using Context, SourceFile): Tree =
    Select(Select(rootDot("org".toTermName), "slf4j".toTermName), "LoggerFactory".toTermName)
}
