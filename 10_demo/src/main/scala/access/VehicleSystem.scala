package dev.coilix.demo
package access

import dev.coilix.demo.vehicle.Vehicle
import java.time.Instant

/** Authorizes mechanics to work on vehicles. */
trait VehicleSystem {

  /** A signed token with the grants `mechanic` gets on `vehicle` at the time `now`, or why access is refused. */
  def authorize(mechanic: Mechanic, vehicle: Vehicle, requested: Set[Grant], now: Instant): Either[AccessError, AuthToken]
}
