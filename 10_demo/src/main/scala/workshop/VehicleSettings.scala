package dev.coilix.demo
package workshop

import dev.coilix.demo.vehicle.Vin

/** A vehicle in the workshop, from `workshop.vehicles`. */
final case class VehicleSettings(vin: Vin, model: String, modules: List[ModuleSettings])
