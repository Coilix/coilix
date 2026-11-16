package dev.coilix.demo
package access

import dev.coilix.demo.vehicle.Vehicle

/** Keys that vehicles trust. */
trait PublicKeyRepository {

  /** The workshop key that `vehicle` trusts. */
  def getKey(vehicle: Vehicle): Either[AccessError, PublicKey]
}
