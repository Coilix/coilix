package dev.coilix.diagnostics

import dev.coilix.context.CoilixModule

/**
 * Adds the health of the application and the checks of its threads. `coilix-diagnostics` comes with `coilix-boot`, and it can
 * be disabled with `coilix.modules.diagnostics.enabled = false`.
 */
final class DiagnosticsModule extends CoilixModule {

  override def name: String = "diagnostics"

  override def components: List[Class[?]] = List(classOf[DiagnosticsConfiguration])
}
