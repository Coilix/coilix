package dev.coilix.demo
package workshop

import dev.coilix.processing.ProcessorId

/** The background processors of the workshop. */
sealed trait WorkshopProcessor extends ProcessorId

/** The identifiers of the background processors of the workshop. */
object WorkshopProcessor {

  /** Exports the fault codes of the fleet, configured under `coilix.processors.fault-code-export`. */
  case object FaultCodeExport extends WorkshopProcessor

  /** Sends service reminders from the mileage of the fleet, configured under `coilix.processors.service-reminders`. */
  case object ServiceReminders extends WorkshopProcessor
}
