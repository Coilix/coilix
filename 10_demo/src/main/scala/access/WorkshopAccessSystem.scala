package dev.coilix.demo
package access

import dev.coilix.annotation.Service
import dev.coilix.annotation.Value
import dev.coilix.common.validation.Validator
import dev.coilix.demo.vehicle.Vehicle
import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

/**
 * Validates the request, asks the policy and signs a token for a new session.
 *
 * @param policy
 *   the primary policy, [[dev.coilix.demo.access.CertificationAccessPolicy]]
 * @param tokenTtl
 *   how long a token is valid, from `workshop.token-ttl`
 * @param maxGrants
 *   most grants in one request, from `workshop.max-grants`
 */
@Service
final class WorkshopAccessSystem(
  policy:                                       AccessPolicy,
  issuer:                                       TokenIssuer,
  @Value("${workshop.token-ttl:5m}") tokenTtl:  FiniteDuration,
  @Value("${workshop.max-grants:8}") maxGrants: Int
) extends VehicleSystem {

  override def authorize(
    mechanic:  Mechanic,
    vehicle:   Vehicle,
    requested: Set[Grant],
    now:       Instant
  ): Either[AccessError, AuthToken] =
    for {
      _        <- validator(vehicle).validate(requested).left.map(AccessError.InvalidRequest(_))
      approved <- policy.approve(mechanic, vehicle, requested)
      token    <- issuer.issue(claims(mechanic, vehicle, approved, now))
    } yield token

  private def validator(vehicle: Vehicle): Validator[Set[Grant]] =
    Validator[Set[Grant]]
      .test(_.nonEmpty)(GrantViolations.noGrants)
      .test(_.sizeIs <= maxGrants)(GrantViolations.tooManyGrants(maxGrants))
      .test(_.forall(grant => vehicle.modules.contains(grant.module)))(GrantViolations.unknownModule)

  private def claims(mechanic: Mechanic, vehicle: Vehicle, approved: Set[Grant], now: Instant): TokenClaims =
    TokenClaims(mechanic.id, vehicle.vin, SessionId(UUID.randomUUID().toString), approved, now, now.plusMillis(tokenTtl.toMillis))
}
