package dev.coilix.demo
package workshop

import dev.coilix.demo.vehicle.Vin

/**
 * The service jobs of a vehicle, in the shape `F`: `JobLog[Option]` gives the latest job and `JobLog[List]` all of them.
 *
 * Both implementations are beans of the same trait, told apart by the type argument.
 */
trait JobLog[F[_]] {

  /** The jobs of `vin`. */
  def jobs(vin: Vin): F[String]
}
