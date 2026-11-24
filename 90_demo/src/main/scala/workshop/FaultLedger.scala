package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.demo.vehicle.Vin
import java.util.concurrent.atomic.AtomicReference

/** The fault codes exported by [[dev.coilix.demo.workshop.FaultCodeExport]], safe to write from its shards at once. */
@Component
final class FaultLedger {

  private val exported = AtomicReference(Map.empty[Vin, List[String]])

  /** Records the fault codes of the vehicle. */
  def record(vin: Vin, faults: List[String]): Unit = exported.getAndUpdate(_.updated(vin, faults)): Unit

  /** The fault codes of each exported vehicle. */
  def faults: Map[Vin, List[String]] = exported.get
}
