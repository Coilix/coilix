package dev.coilix.slf4s

import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.plugins.PluginPhase

/**
 * The compiler phase `slf4s`, which runs after parsing and before type checking, so the added field is type checked like
 * hand-written code. Java sources are skipped.
 *
 * @param transformer
 *   rewrites the syntax tree of each Scala compilation unit
 */
final class Slf4sPhase(transformer: Slf4sTreeTransformer) extends PluginPhase {
  override def phaseName: String = "slf4s"

  override def runsAfter: Set[String] = Set("parser")

  override def runsBefore: Set[String] = Set("typer")

  override def runOn(units: List[CompilationUnit])(using Context): List[CompilationUnit] =
    units.map(transformUnit)

  private def transformUnit(unit: CompilationUnit)(using Context): CompilationUnit = {
    if (!unit.isJava) unit.untpdTree = transformer.transform(unit.untpdTree, unit.source)
    unit
  }
}
