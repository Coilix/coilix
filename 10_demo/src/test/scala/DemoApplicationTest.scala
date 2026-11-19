package dev.coilix.demo

import dev.coilix.boot.Coilix
import dev.coilix.demo.access.AccessPolicy
import dev.coilix.demo.access.Certification
import dev.coilix.demo.access.CertificationAccessPolicy
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.VehicleSystem
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleKind
import dev.coilix.demo.vehicle.VehicleRegistry
import dev.coilix.demo.workshop.WorkshopProperties
import java.time.Instant
import munit.FunSuite

final class DemoApplicationTest extends FunSuite {
  private val context = Coilix.run[DemoApplication.type](Array("--mechanic=M-002", "--workshop.max-grants=7"))

  override def afterAll(): Unit = context.close()

  test("registers every demo component") {
    assertEquals(
      context.beanNames.sorted,
      List(
        "applicationArguments",
        "applicationContext",
        "brakesDriver",
        "certificationAccessPolicy",
        "clock",
        "ed25519TokenIssuer",
        "ed25519TokenVerifier",
        "engineDriver",
        "environment",
        "immobilizerDriver",
        "inMemoryReplayGuard",
        "oemKeyRegistry",
        "ownerApp",
        "readOnlyAccessPolicy",
        "securityDrills",
        "silentNotifier",
        "vehicleRegistry",
        "workshopAccessSystem",
        "workshopConfiguration",
        "workshopKeys",
        "workshopProperties",
        "workshopShift"
      )
    )
  }

  test("binds the workshop from application.yaml") {
    val workshop = context.getBean[WorkshopProperties]
    assertEquals(
      workshop.mechanics.map(_.certification),
      List(Certification.Master, Certification.Technician, Certification.Apprentice)
    )
    assertEquals(workshop.vehicles.map(_.vin.value), List("WVWZZZ1KZ6W000001", "TMBJJ7NE8J0000002"))
    assertEquals(
      workshop.vehicles.flatMap(_.modules.map(_.kind)).distinct,
      List(ModuleKind.Engine, ModuleKind.Brakes, ModuleKind.Immobilizer)
    )
    assertEquals(workshop.oemPortal.apiKey.toString, "*****")
  }

  test("injects the primary access policy") {
    assertEquals(context.getBean[AccessPolicy].getClass, classOf[CertificationAccessPolicy])
  }

  test("issues tokens valid for the configured time") {
    val registry = context.getBean[VehicleRegistry]
    val now      = Instant.parse("2026-11-16T10:00:00Z")
    val token    = registry.all.headOption.toRight("no vehicle").flatMap { vehicle =>
      context.getBean[VehicleSystem].authorize(
        context.getBean[WorkshopProperties].mechanics.head,
        vehicle,
        Set(Grant(TestWorkshop.engine, Action.ReadFaults)),
        now
      )
    }
    assertEquals(token.map(_.claims.expiresAt), Right(now.plusSeconds(300)))
  }
}
