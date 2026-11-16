package dev.coilix.demo
package access

import dev.coilix.demo.vehicle.Vehicle

/** Decides which grants a mechanic gets. */
trait AccessPolicy {

  /** The grants `mechanic` gets on `vehicle`, or why they are refused. */
  def approve(mechanic: Mechanic, vehicle: Vehicle, requested: Set[Grant]): Either[AccessError, Set[Grant]]
}
