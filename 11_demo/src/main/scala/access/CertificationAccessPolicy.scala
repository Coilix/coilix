package dev.coilix.demo
package access

import dev.coilix.annotation.Primary
import dev.coilix.annotation.Service
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.Vehicle

/**
 * Grants actions by the certification of the mechanic.
 *
 * Marked with `@Primary`, so it is injected wherever one [[dev.coilix.demo.access.AccessPolicy]] is needed, although
 * [[dev.coilix.demo.access.ReadOnlyAccessPolicy]] is a bean too.
 */
@Service
@Primary
final class CertificationAccessPolicy extends AccessPolicy {

  override def approve(mechanic: Mechanic, vehicle: Vehicle, requested: Set[Grant]): Either[AccessError, Set[Grant]] =
    Either.cond(
      requested.forall(grant => allowed(mechanic.certification).contains(grant.action)),
      requested,
      AccessError.NotCertified
    )

  private def allowed(certification: Certification): Set[Action] =
    certification match {
      case Certification.Apprentice => Set(Action.ReadFaults, Action.ReadLiveData)
      case Certification.Technician => Set(Action.ReadFaults, Action.ReadLiveData, Action.RunTest, Action.Adapt)
      case Certification.Master     => Action.values.toSet
    }
}
