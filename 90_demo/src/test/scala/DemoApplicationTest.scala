package dev.coilix.demo

import dev.coilix.demo.access.AccessPolicy
import dev.coilix.demo.access.Certification
import dev.coilix.demo.access.CertificationAccessPolicy
import dev.coilix.demo.access.Grant
import dev.coilix.demo.access.VehicleSystem
import dev.coilix.demo.vehicle.Action
import dev.coilix.demo.vehicle.ModuleKind
import dev.coilix.demo.vehicle.VehicleRegistry
import dev.coilix.demo.workshop.BayMonitor
import dev.coilix.demo.workshop.TelematicsUplink
import dev.coilix.demo.workshop.WorkshopProperties
import dev.coilix.testkit.CoilixSuite
import dev.coilix.testkit.CoilixTestSetup
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import munit.FunSuite

final class DemoApplicationTest extends FunSuite with CoilixSuite {
  private given workshop: CoilixTestSetup = CoilixTestSetup.forApplication[DemoApplication.type]

  private val now    = Instant.parse("2026-11-16T10:00:00Z")
  private val shared = coilixSuiteContext(workshop.withArguments("--mechanic=M-002", "--workshop.max-grants=7"))

  override def munitFixtures = List(shared)

  shared.test("registers every demo component") {
    assertEquals(
      context.beanNames.sorted,
      List(
        "aftermarketPartsSupplier",
        "applicationArguments",
        "applicationContext",
        "bayMonitor",
        "blockingExecutor",
        "bookingDesk",
        "bootConfiguration",
        "brakesDriver",
        "catsEffectConfiguration",
        "catsEffectSettings",
        "certificationAccessPolicy",
        "checkInLog",
        "claimSweeper",
        "clock",
        "clusterConfiguration",
        "clusterSettings",
        "coilixIORuntime",
        "collectedComponents",
        "concurrencyConfiguration",
        "concurrencySettings",
        "contextClosingPublisher",
        "deadlockCheck",
        "diagnosticsConfiguration",
        "diagnosticsMonitor",
        "diagnosticsSettings",
        "ed25519TokenIssuer",
        "ed25519TokenVerifier",
        "engineDriver",
        "environment",
        "eventBus",
        "eventRecorder",
        "eventsConfiguration",
        "eventsSettings",
        "fatalErrorHandler",
        "fatalErrorSettings",
        "faultLedger",
        "fiberEngine",
        "fileJobArchive",
        "findingReporter",
        "fleetScan",
        "fullJobLog",
        "health",
        "immobilizerDriver",
        "inMemoryReplayGuard",
        "inspectionSheet",
        "ioRunnerAdapter",
        "ioRuntime",
        "ioTaskExecutor",
        "latestJobLog",
        "liftBoard",
        "liftHealth",
        "logLevels",
        "loggingConfiguration",
        "loggingSettings",
        "longBlockedCheck",
        "mdcContextPropagator",
        "moduleCensus",
        "oemKeyRegistry",
        "oemPartsSupplier",
        "ownerApp",
        "partsQuote",
        "portalConfiguration",
        "portalSession",
        "processTerminator",
        "processingConfiguration",
        "processingSettings",
        "processorManager",
        "readOnlyAccessPolicy",
        "recallBoard",
        "recallBriefing",
        "recallBulletin",
        "recallCheck",
        "replicaIdentity",
        "scheduledMethodRegistrar",
        "securityDrills",
        "serviceDesk",
        "shiftStatistics",
        "silentNotifier",
        "staffNotifications",
        "startupHealthIndicator",
        "startupHealthListener",
        "testerCrashDrill",
        "threadCountCheck",
        "threadGrowthCheck",
        "threadsHealthIndicator",
        "troubleshootingDesk",
        "vehicleRegistry",
        "warrantyRegistry",
        "workshopAccessSystem",
        "workshopConfiguration",
        "workshopKeys",
        "workshopOpening",
        "workshopProperties",
        "workshopShift"
      )
    )
  }

  shared.test("binds the workshop from application.yaml") {
    val workshop = bean[WorkshopProperties]
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

  shared.test("checks the bays on a fiber of its own") {
    assert(LazyList.range(0, 100).exists(_ => bean[BayMonitor].checksDone > 0 || { Thread.sleep(20); false }))
  }

  shared.test("reads the command-line options") {
    assertEquals((arguments.optionValue("mechanic"), property[Int]("workshop.max-grants")), (Some("M-002"), Some(7)))
  }

  shared.test("injects the primary access policy") {
    assertEquals(bean[AccessPolicy].getClass, classOf[CertificationAccessPolicy])
  }

  shared.test("issues tokens valid for the configured time") {
    val token = bean[VehicleRegistry].all.headOption.toRight("no vehicle").flatMap { vehicle =>
      bean[VehicleSystem].authorize(
        bean[WorkshopProperties].mechanics.head,
        vehicle,
        Set(Grant(TestWorkshop.engine, Action.ReadFaults)),
        now
      )
    }
    assertEquals(token.map(_.claims.expiresAt), Right(now.plusSeconds(300)))
  }

  coilixTest("starts without a telematics uplink") {
    assertEquals(beanOption[TelematicsUplink], None)
  }

  coilixTest(
    "the shift uses the fixed clock and reports through the added uplink",
    workshop.replaceBean[Clock](Clock.fixed(now, ZoneOffset.UTC)).addBean[TelematicsUplink](RecordingUplink()).withRunners
  ) {
    assertEquals(bean[Clock].instant(), now)
    assertEquals(
      beans[TelematicsUplink].collect { case uplink: RecordingUplink => uplink.reports },
      List(List("Inspection of WVWZZZ1KZ6W000001 done"))
    )
  }
}
