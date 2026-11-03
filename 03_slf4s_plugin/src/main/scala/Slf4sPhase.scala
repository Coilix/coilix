package dev.coilix.slf4s

import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.plugins.PluginPhase

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
