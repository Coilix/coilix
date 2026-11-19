package dev.coilix.demo
package workshop

import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner
import dev.coilix.demo.access.AccessError
import dev.coilix.demo.access.AuthToken
import dev.coilix.demo.access.Certification
import dev.coilix.demo.access.Ed25519TokenIssuer
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.Mechanic
import dev.coilix.demo.access.SessionId
import dev.coilix.demo.access.TokenClaims
import dev.coilix.demo.access.TokenIssuer
import dev.coilix.demo.access.VehicleSystem
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleCommand
import dev.coilix.demo.vehicle.ModuleId
import dev.coilix.demo.vehicle.RequestId
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.VehicleRegistry
import java.security.KeyPairGenerator
import java.time.Clock

/**
 * Tries the attacks and mistakes the access checks must stop, and logs whether each one was refused for the right reason.
 *
 * Runs second because of `@Order(2)`. It needs two vehicles, a master and an apprentice in the configuration.
 */
@Component
@Order(2)
@Slf4s
final class SecurityDrills(
  properties: WorkshopProperties,
  registry:   VehicleRegistry,
  system:     VehicleSystem,
  issuer:     TokenIssuer,
  clock:      Clock
) extends ApplicationRunner {
  private val engine = ModuleId("engine")
  private val abs    = ModuleId("abs")

  override def run(arguments: ApplicationArguments): Unit =
    (registry.all, withCertification(Certification.Master), withCertification(Certification.Apprentice)) match {
      case (vehicle :: other :: _, Some(master), Some(apprentice)) => drill(vehicle, other, master, apprentice)
      case _                                                       => log.warn("Security drills need two vehicles, a master and an apprentice")
    }

  private def withCertification(certification: Certification): Option[Mechanic] =
    properties.mechanics.find(_.certification == certification)

  private def drill(vehicle: Vehicle, other: Vehicle, master: Mechanic, apprentice: Mechanic): Unit = {
    report(
      "Apprentice asks to code the engine",
      AccessError.NotCertified,
      system.authorize(apprentice, vehicle, Set(Grant(engine, Action.Code)), now)
    )
    report("Request without grants", AccessError.InvalidRequest(Nil), system.authorize(master, vehicle, Set.empty, now))
    report(
      "Rogue workshop signs its own token",
      AccessError.InvalidSignature,
      rogueToken(master, vehicle).flatMap(read(vehicle, _, "D-1"))
    )
    report("Signed grant for an action the module lacks", AccessError.ModuleRejected, absCoding(master, vehicle))
    system
      .authorize(master, vehicle, Set(Grant(engine, Action.ReadFaults)), now)
      .fold(error => log.error(s"Drills could not get a token: $error"), token => tokenDrills(vehicle, other, token))
  }

  private def tokenDrills(vehicle: Vehicle, other: Vehicle, token: AuthToken): Unit = {
    val _ = read(vehicle, token, "D-2")
    report("Recorded request sent again", AccessError.ReplayedRequest, read(vehicle, token, "D-2"))
    report("Token used on another vehicle", AccessError.WrongVehicle, read(other, token, "D-3"))
    report(
      "Token used after it expired",
      AccessError.TokenExpired,
      vehicle.program(token, engine, readFaults("D-4"), token.claims.expiresAt.plusSeconds(1))
    )
    report(
      "Grant added to a signed token",
      AccessError.InvalidSignature,
      vehicle.program(widened(token), engine, coding("D-5"), now)
    )
    report("Action the token does not grant", AccessError.MissingGrant, vehicle.program(token, engine, coding("D-6"), now))
  }

  private def rogueToken(master: Mechanic, vehicle: Vehicle): Either[AccessError, AuthToken] =
    Ed25519TokenIssuer(KeyPairGenerator.getInstance("Ed25519").generateKeyPair()).issue(claims(
      master,
      vehicle,
      Grant(engine, Action.ReadFaults)
    ))

  private def absCoding(master: Mechanic, vehicle: Vehicle): Either[AccessError, ?] =
    issuer.issue(claims(master, vehicle, Grant(abs, Action.Code))).flatMap(vehicle.program(_, abs, coding("D-7"), now))

  private def widened(token: AuthToken): AuthToken =
    token.copy(claims = token.claims.copy(grants = token.claims.grants + Grant(engine, Action.Code)))

  private def claims(master: Mechanic, vehicle: Vehicle, grant: Grant): TokenClaims =
    TokenClaims(master.id, vehicle.vin, SessionId("drill"), Set(grant), now, now.plusSeconds(60))

  private def read(vehicle: Vehicle, token: AuthToken, requestId: String): Either[AccessError, ?] =
    vehicle.program(token, engine, readFaults(requestId), now)

  private def readFaults(requestId: String): ModuleCommand = ModuleCommand(Action.ReadFaults, RequestId(requestId))

  private def coding(requestId: String): ModuleCommand =
    ModuleCommand(Action.Code, RequestId(requestId), Map("injectors" -> "C1 C2 C3 C4"))

  private def now = clock.instant()

  private def report(name: String, expected: AccessError, outcome: Either[AccessError, ?]): Unit =
    outcome match {
      case Left(error) if error.ordinal == expected.ordinal => log.info(s"Drill '$name': refused with $error")
      case other                                            => log.error(s"Drill '$name': expected $expected, but got $other")
    }
}
