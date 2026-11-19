package dev.coilix.demo
package diagnostics

import dev.coilix.context.CoilixModule

/**
 * An optional module of the demo, listed in `META-INF/services/dev.coilix.context.CoilixModule`.
 *
 * The demo scans only its `access`, `vehicle` and `workshop` packages, so [[dev.coilix.demo.diagnostics.FleetScan]] becomes a
 * bean through this module. `--coilix.modules.diagnostics.enabled=false` leaves it out.
 */
final class DiagnosticsModule extends CoilixModule {

  override def name: String = "diagnostics"

  override def components: List[Class[?]] = List(classOf[FleetScan])
}
