package io.github.coilix.slf4s

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.plugins.StandardPlugin

final class Slf4sPlugin extends StandardPlugin {
  override val name:        String = "slf4s"
  override val description: String = "Adds a slf4j logger field named log to definitions annotated with @Slf4s"

  override def initialize(options: List[String])(using Context): List[PluginPhase] =
    List(Slf4sPhase(Slf4sTreeTransformer.default))
}
