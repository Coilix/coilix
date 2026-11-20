package dev.coilix.demo

import dev.coilix.context.ProxiedBean
import dev.coilix.demo.access.ClaimSweeper
import dev.coilix.demo.diagnostics.FleetScan
import dev.coilix.demo.vehicle.Vin
import dev.coilix.demo.workshop.BayMonitor
import dev.coilix.demo.workshop.FileJobArchive
import dev.coilix.demo.workshop.JobArchive
import dev.coilix.demo.workshop.JobLog
import dev.coilix.demo.workshop.JobNotes
import dev.coilix.demo.workshop.Notifier
import dev.coilix.demo.workshop.PartsQuote
import dev.coilix.demo.workshop.ServiceDesk
import dev.coilix.demo.workshop.ShiftStatistics
import dev.coilix.demo.workshop.TelematicsUplink
import dev.coilix.demo.workshop.WorkshopOpening
import dev.coilix.events.EventBus
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import munit.FunSuite
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class DemoFeaturesTest extends FunSuite with CoilixContextOps {
  private val workshop = CoilixTestSetup.forApplication[DemoApplication.type]
  private val vin      = Vin("WVWZZZ1KZ6W000001")

  test("quotes a part from the primary supplier and from the one named by @Qualifier") {
    workshop.use {
      assertEquals(bean[PartsQuote].compare("brake pads"), "brake pads: OEM 50, aftermarket 35")
    }
  }

  test("tells the job logs apart by their type argument") {
    workshop.use {
      assertEquals((bean[JobLog[Option]].jobs(vin), bean[JobLog[List]].jobs(vin).size), (Some("inspection 2026-11"), 2))
    }
  }

  test("updates the job notes with EditOption") {
    workshop.use {
      assertEquals(bean[ServiceDesk].updatedNotes, JobNotes(Some("Brake pads replaced"), None))
    }
  }

  test("archives a report on a task thread through the @Async proxy") {
    workshop.use {
      val archive = bean[JobArchive]
      assert(ProxiedBean.unwrap(archive).isInstanceOf[FileJobArchive])
      assert(Await.result(archive.archive("test"), 5.seconds).contains("archived on coilix-task-"))
    }
  }

  test("scans the fleet with the bean of the diagnostics module") {
    workshop.use {
      assertEquals(bean[FleetScan].scan().map(_.takeWhile(_ != ':')), List("WVWZZZ1KZ6W000001", "TMBJJ7NE8J0000002"))
    }
  }

  test("leaves the diagnostics module out when it is disabled") {
    workshop.withProperties("coilix.modules.diagnostics.enabled" -> "false").use {
      assertEquals(beanOption[FleetScan], None)
    }
  }

  test("opens the workshop when the context starts") {
    workshop.use {
      assert(bean[WorkshopOpening].isOpen)
    }
  }

  test("stops the sweeping before the accepted work and closes the workshop last") {
    val probe              = DrainProbe()
    val (opening, sweeper) = workshop.addBean(probe).use((bean[WorkshopOpening], bean[ClaimSweeper]))
    assertEquals(probe.sweepingAndOpenWhileDraining, Some((false, true)))
    assertEquals((opening.isOpen, sweeper.isSweeping), (false, false))
  }

  test("has no telematics uplink unless it is enabled") {
    workshop.use {
      assertEquals(beanOption[TelematicsUplink], None)
    }
    workshop.withProperties("workshop.telematics.enabled" -> "true").use {
      assert(beanOption[TelematicsUplink].isDefined)
    }
  }

  test("each replica checks its own bays") {
    workshop
      .withProperties(
        "coilix.cluster.identity"      -> "static",
        "coilix.cluster.replicas"      -> "2",
        "coilix.cluster.replica-index" -> "1"
      )
      .use {
        assertEquals(bean[BayMonitor].myBays, List(1, 3))
      }
  }

  test("counts a finished inspection at once and notifies the staff asynchronously") {
    val notifier = RecordingNotifier()
    workshop.replaceBean[Notifier](notifier).withRunners.use {
      assert(bean[EventBus].awaitIdle(5.seconds))
      assertEquals(
        (bean[ShiftStatistics].inspectionsDone, notifier.messages),
        (1, List("Anna Nowak finished the inspection of WVWZZZ1KZ6W000001"))
      )
    }
  }

  test("a clock added by the test replaces the default one") {
    val fixed = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC)
    workshop.addBean[Clock](fixed).use {
      assertEquals(beans[Clock], List(fixed))
    }
  }
}
