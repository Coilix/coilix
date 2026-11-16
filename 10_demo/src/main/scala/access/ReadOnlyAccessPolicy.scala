package dev.coilix.demo
package access

import dev.coilix.annotation.Service
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.Vehicle

/** Grants only reading, whoever asks. Used by the owner app, which injects it by its class. */
@Service
final class ReadOnlyAccessPolicy extends AccessPolicy {
  private val reading = Set(Action.ReadFaults, Action.ReadLiveData)

  override def approve(mechanic: Mechanic, vehicle: Vehicle, requested: Set[Grant]): Either[AccessError, Set[Grant]] =
    Either.cond(requested.forall(grant => reading.contains(grant.action)), requested, AccessError.NotCertified)
}
