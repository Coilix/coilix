package dev.coilix.slf4s

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.plugins.StandardPlugin

/**
 * Scala 3 compiler plugin that adds an SLF4J logger field named `log` to classes and objects annotated with `@Slf4s`.
 *
 * Enable it with the compiler options `-Xplugin:<path to coilix-slf4s-plugin.jar>` and `-Xplugin-require:slf4s`. The second
 * option makes compilation fail when the plugin is missing, instead of reporting `log` as an unknown name.
 *
 * ```scala
 * @Slf4s
 * final class OrderService {
 *   def place(order: Order): Unit = log.info(s"Placing order ${order.id}")
 * }
 * ```
 */
final class Slf4sPlugin extends StandardPlugin {
  override val name:        String = "slf4s"
  override val description: String = "Adds a slf4j logger field named log to definitions annotated with @Slf4s"

  override def initialize(options: List[String])(using Context): List[PluginPhase] =
    List(Slf4sPhase(Slf4sTreeTransformer.default))
}
