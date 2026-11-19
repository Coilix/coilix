package dev.coilix.demo

import dev.coilix.demo.access.AccessPolicy
import dev.coilix.demo.access.Certification
import dev.coilix.demo.access.CertificationAccessPolicy
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.VehicleSystem
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleKind
import dev.coilix.demo.vehicle.VehicleRegistry
import dev.coilix.demo.workshop.TelematicsUplink
import dev.coilix.demo.workshop.WorkshopProperties
import dev.coilix.testkit.CoilixFixtures
import dev.coilix.testkit.CoilixTestSetup
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import munit.FunSuite

final class DemoApplicationTest extends FunSuite with CoilixFixtures {
  private val workshop = CoilixTestSetup.forApplication[DemoApplication.type]
  private val shared   = coilixSuiteContext(workshop.withArguments("--mechanic=M-002", "--workshop.max-grants=7"))
  private val now      = Instant.parse("2026-11-16T10:00:00Z")
  private val inShift  = coilixContext(
    workshop.replaceBean[Clock](Clock.fixed(now, ZoneOffset.UTC)).addBean[TelematicsUplink](RecordingUplink()).withRunners
  )

  override def munitFixtures = List(shared)

  private def context = shared()

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

  inShift.test("the shift uses the fixed clock and reports through the added uplink") { context =>
    assertEquals(context.getBean[Clock].instant(), now)
    assertEquals(
      context.getBeansOfType[TelematicsUplink].collect { case uplink: RecordingUplink => uplink.reports },
      List(List("Inspection of WVWZZZ1KZ6W000001 done"))
    )
  }
}
