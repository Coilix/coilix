package dev.coilix.demo

import dev.coilix.config.valuetype.Secret
import dev.coilix.demo.access.AuthToken
import dev.coilix.demo.access.Certification
import dev.coilix.demo.access.Ed25519TokenIssuer
import dev.coilix.demo.access.Ed25519TokenVerifier
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.InMemoryReplayGuard
import dev.coilix.demo.access.Mechanic
import dev.coilix.demo.access.MechanicId
import dev.coilix.demo.access.OemKeyRegistry
import dev.coilix.demo.access.SessionId
import dev.coilix.demo.access.TokenClaims
import dev.coilix.demo.vehicle.BrakesDriver
import dev.coilix.demo.vehicle.EngineDriver
import dev.coilix.demo.vehicle.ImmobilizerDriver
import dev.coilix.demo.vehicle.ModuleId
import dev.coilix.demo.vehicle.ModuleKind
import dev.coilix.demo.vehicle.Vehicle
import dev.coilix.demo.vehicle.VehicleRegistry
import dev.coilix.demo.vehicle.Vin
import dev.coilix.demo.workshop.ModuleSettings
import dev.coilix.demo.workshop.OemPortalSettings
import dev.coilix.demo.workshop.VehicleSettings
import dev.coilix.demo.workshop.WorkshopProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant

object TestWorkshop {
  val keys: KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

  val golfVin:    Vin = Vin("WVWZZZ1KZ6W000001")
  val octaviaVin: Vin = Vin("TMBJJ7NE8J0000002")

  val engine:      ModuleId = ModuleId("engine")
  val abs:         ModuleId = ModuleId("abs")
  val immobilizer: ModuleId = ModuleId("immobilizer")

  val master:     Mechanic = Mechanic(MechanicId("M-001"), "Anna", Certification.Master)
  val technician: Mechanic = Mechanic(MechanicId("M-002"), "Piotr", Certification.Technician)
  val apprentice: Mechanic = Mechanic(MechanicId("M-003"), "Ola", Certification.Apprentice)

  val now: Instant = Instant.parse("2026-11-16T10:00:00Z")

  val properties: WorkshopProperties =
    WorkshopProperties(
      "Test garage",
      OemPortalSettings("https://oem.test", Secret("portal-key")),
      List(master, technician, apprentice),
      List(
        VehicleSettings(
          golfVin,
          "Golf",
          List(
            ModuleSettings(engine, ModuleKind.Engine, List("P0301")),
            ModuleSettings(abs, ModuleKind.Brakes),
            ModuleSettings(immobilizer, ModuleKind.Immobilizer)
          )
        ),
        VehicleSettings(
          octaviaVin,
          "Octavia",
          List(ModuleSettings(engine, ModuleKind.Engine), ModuleSettings(abs, ModuleKind.Brakes))
        )
      )
    )

  val issuer: Ed25519TokenIssuer = Ed25519TokenIssuer(keys)

  def registry(): VehicleRegistry =
    VehicleRegistry(
      properties,
      List(EngineDriver(), BrakesDriver(), ImmobilizerDriver()),
      OemKeyRegistry(properties, keys),
      Ed25519TokenVerifier(),
      InMemoryReplayGuard()
    )

  def vehicle(vin: Vin): Vehicle =
    registry().find(vin).getOrElse(throw IllegalStateException(s"No test vehicle $vin"))

  def claims(vin: Vin, grants: Grant*): TokenClaims =
    TokenClaims(master.id, vin, SessionId("S-1"), grants.toSet, now, now.plusSeconds(300))

  def token(vin: Vin, grants: Grant*): AuthToken =
    issuer.issue(claims(vin, grants*)).getOrElse(throw IllegalStateException("Signing failed"))
}
