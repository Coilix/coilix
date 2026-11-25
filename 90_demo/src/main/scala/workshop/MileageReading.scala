package dev.coilix.demo
package workshop

import dev.coilix.demo.vehicle.Vin

/** A mileage reported by a connected vehicle. */
final case class MileageReading(vin: Vin, kilometres: Int)
