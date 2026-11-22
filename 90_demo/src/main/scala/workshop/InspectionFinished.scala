package dev.coilix.demo
package workshop

import dev.coilix.demo.vehicle.Vin

/**
 * Published by [[dev.coilix.demo.workshop.WorkshopShift]] when a mechanic has worked through an inspection.
 *
 * @param mechanic
 *   the name of the mechanic
 * @param vin
 *   the inspected vehicle
 * @param steps
 *   the number of steps of the job
 */
final case class InspectionFinished(mechanic: String, vin: Vin, steps: Int)
