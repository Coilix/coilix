package dev.coilix.demo
package workshop

import dev.coilix.processing.ProcessorId

/** The background processors of the workshop. */
sealed trait WorkshopProcessor extends ProcessorId

/** The identifiers of the background processors of the workshop. */
object WorkshopProcessor {

  /** Exports the fault codes of the fleet, configured under `coilix.processors.fault-code-export`. */
  case object FaultCodeExport extends WorkshopProcessor

  /** Checks in the vehicles of the fleet as a stream, configured under `coilix.processors.vehicle-check-in`. */
  case object VehicleCheckIn extends WorkshopProcessor
}
