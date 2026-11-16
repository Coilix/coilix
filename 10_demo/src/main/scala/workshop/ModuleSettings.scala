package dev.coilix.demo
package workshop

import dev.coilix.demo.vehicle.ModuleId
import dev.coilix.demo.vehicle.ModuleKind

/**
 * A control module of a configured vehicle.
 *
 * @param faults
 *   fault codes the simulated module reports; none when the key is missing
 */
final case class ModuleSettings(id: ModuleId, kind: ModuleKind, faults: List[String] = Nil)
