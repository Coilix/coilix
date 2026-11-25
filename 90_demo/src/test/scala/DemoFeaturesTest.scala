package dev.coilix.demo

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.boot.CoilixIOApp
import dev.coilix.boot.CoilixIOStartup
import dev.coilix.concurrency.ProcessTerminator
import dev.coilix.context.ProxiedBean
import dev.coilix.demo.access.ClaimSweeper
import dev.coilix.demo.diagnostics.FleetScan
import dev.coilix.demo.vehicle.Vin
import dev.coilix.demo.workshop.BayMonitor
import dev.coilix.demo.workshop.BookingDesk
import dev.coilix.demo.workshop.FaultCodeExport
import dev.coilix.demo.workshop.FaultLedger
import dev.coilix.demo.workshop.FileJobArchive
import dev.coilix.demo.workshop.InspectionFinished
import dev.coilix.demo.workshop.InspectionSheet
import dev.coilix.demo.workshop.JobArchive
import dev.coilix.demo.workshop.JobLog
import dev.coilix.demo.workshop.JobNotes
import dev.coilix.demo.workshop.LiftBoard
import dev.coilix.demo.workshop.ModuleCensus
import dev.coilix.demo.workshop.Notifier
import dev.coilix.demo.workshop.PartsQuote
import dev.coilix.demo.workshop.PortalSession
import dev.coilix.demo.workshop.RecallBoard
import dev.coilix.demo.workshop.RecallBriefing
import dev.coilix.demo.workshop.RecallCheck
import dev.coilix.demo.workshop.ServiceDesk
import dev.coilix.demo.workshop.ShiftStatistics
import dev.coilix.demo.workshop.TelematicsUplink
import dev.coilix.demo.workshop.WorkshopOpening
import dev.coilix.demo.workshop.WorkshopProcessor
import dev.coilix.diagnostics.Health
import dev.coilix.diagnostics.HealthStatus
import dev.coilix.events.ApplicationStarted
import dev.coilix.events.ContextReady
import dev.coilix.events.EventBus
import dev.coilix.processing.ProcessorManager
import dev.coilix.processing.WorkerState
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import munit.FunSuite
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Deadline
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

  test("archives a report on the blocking executor through the @Async proxy") {
    workshop.use {
      val archive = bean[JobArchive]
      assert(ProxiedBean.unwrap(archive).isInstanceOf[FileJobArchive])
      assert(Await.result(archive.archive("test"), 5.seconds).contains("archived on coilix-blocking-"))
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

  test("runs the inspection between the startup events and notifies the staff in the thread of the test") {
    val notifier = RecordingNotifier()
    workshop.replaceBean[Notifier](notifier).withRunners.withSynchronousEvents.use {
      assertEquals(
        (
          recordedEvents[AnyRef].map(_.getClass.getSimpleName),
          recordedEvents[InspectionFinished].map(_.steps),
          notifier.messages.size
        ),
        (List("ContextReady", "InspectionFinished", "ApplicationStarted"), List(6), 1)
      )
      assertEquals((recordedEvents[ContextReady].size, recordedEvents[ApplicationStarted].size), (1, 1))
    }
  }

  test("a tester that runs out of memory shuts the workshop down with exit code 70") {
    val terminator = RecordingTerminator()
    workshop
      .replaceBean[ProcessTerminator](terminator)
      .withProperties("coilix.fatal-errors.action" -> "shutdown")
      .withArguments("--simulate-fatal-error")
      .withRunners
      .use {
        assert(terminator.exited.await(10, TimeUnit.SECONDS))
        assertEquals((terminator.recorded, bean[WorkshopOpening].isOpen), (List("exit 70"), false))
      }
  }

  test("the asynchronous notification keeps the job of the inspection in the MDC") {
    val notifier = RecordingNotifier()
    workshop.replaceBean[Notifier](notifier).withRunners.use {
      assert(awaitEvents())
      assertEquals(notifier.jobs, List(Some("inspection-WVWZZZ1KZ6W000001")))
    }
  }

  test("every inspection gets a new inspection sheet") {
    workshop.use {
      assert(!(bean[InspectionSheet] eq bean[InspectionSheet]))
    }
  }

  test("issues tokens only after the workshop key is registered at the OEM portal") {
    workshop.use {
      val created = summon[dev.coilix.context.ApplicationContext].createdBeans.map(_.beanName)
      assert(created.indexOf("oemKeyRegistry") < created.indexOf("ed25519TokenIssuer"), created)
    }
  }

  test("the warranty registry is loaded only when the front desk needs it") {
    def loaded(using context: dev.coilix.context.ApplicationContext): Boolean =
      context.createdBeans.exists(_.beanName == "warrantyRegistry")
    assertEquals((workshop.use(loaded), workshop.withRunners.use(loaded)), (false, true))
  }

  test("checks recalls of the whole fleet with IO, waiting for the portal on the blocking executor") {
    workshop.use {
      val lines = bean[RecallCheck].openRecalls()
      assertEquals(
        (lines.map(_.takeWhile(_ != ':')), lines.forall(_.contains("coilix-blocking-"))),
        (List("WVWZZZ1KZ6W000001", "TMBJJ7NE8J0000002"), true)
      )
    }
  }

  test("logs in to the portal with a Resource bean, downloads the bulletin with an IO bean and logs out when closing") {
    val (session, lines) = workshop.use((bean[PortalSession], bean[RecallCheck].openRecalls()))
    assertEquals(
      (session.isOpen, lines.forall(_.contains("in bulletin 2026-11")), session.url),
      (false, true, "https://oem-portal.example.com/workshop-keys")
    )
  }

  test("pins the recall bulletin with an IO @PostConstruct method and takes it down with an IO @PreDestroy method") {
    val (board, notice) = workshop.use((bean[RecallBoard], bean[RecallBoard].pinned))
    assertEquals((notice, board.pinned), (Some("Recall bulletin 2026-11"), None))
  }

  test("reads the recall notice to the staff with an IO runner among the other runners") {
    assertEquals(workshop.withRunners.use(bean[RecallBriefing].isDone), true)
  }

  test("starts as a CoilixIOApp on the given runtime, runs the shift and closes after the runners") {
    val runtime = IORuntime.builder().build()
    try {
      val sameRuntime = CoilixIOStartup
        .application[DemoApplication.type](Nil, runtime)
        .use(application => IO(application.context.getBean[IORuntime] eq runtime))
      val result      = sameRuntime.product(CoilixIOStartup.run[DemoApplication.type](Nil, runtime)).unsafeRunSync()(using runtime)
      assertEquals(result, (true, ExitCode.Success))
    } finally {
      runtime.shutdown()
    }
  }

  test("counts the modules of the fleet with a Pekko stream on the materializer of the pekko module") {
    workshop.use {
      assertEquals(Await.result(bean[ModuleCensus].count(), 5.seconds), Map("engine" -> 2, "abs" -> 2, "immobilizer" -> 1))
    }
  }

  test("the compute pool of cats-effect is the only compute pool of the workshop") {
    workshop.use {
      val thread = Await.result(bean[dev.coilix.concurrency.TaskExecutor].submit(Thread.currentThread.getName), 5.seconds)
      assertEquals((thread.startsWith("io-compute-"), beans[dev.coilix.concurrency.TaskExecutor].size), (true, 1))
    }
  }

  test("pauses bookings while every lift is taken, told by the lifts health indicator and HealthChanged") {
    workshop.withSynchronousEvents.use {
      val lifts  = bean[LiftBoard]
      val health = bean[Health]
      val _      = (lifts.occupy(), lifts.occupy())
      health.refresh()
      val full   = (health.readiness, health.liveness, bean[BookingDesk].accepting)
      lifts.release()
      health.refresh()
      assertEquals(
        (full, health.readiness, bean[BookingDesk].accepting),
        ((HealthStatus.Degraded, HealthStatus.Up, false), HealthStatus.Up, true)
      )
    }
  }

  test("a clock added by the test replaces the default one") {
    val fixed = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC)
    workshop.addBean[Clock](fixed).use {
      assertEquals(beans[Clock], List(fixed))
    }
  }

  test("exports the fault codes of the fleet with a background processor on two shards") {
    workshop.use {
      val processors = bean[ProcessorManager]
      assert(completed(processors, 5.seconds.fromNow))
      assertEquals(bean[FaultLedger].faults, Map(vin -> List("P0301", "P0420", "C0035"), Vin("TMBJJ7NE8J0000002") -> Nil))
      assertEquals(
        processors.topology.processors.map(processor => (processor.id, processor.shards.map(_.shard))),
        List((WorkshopProcessor.FaultCodeExport, List(0, 1)))
      )
      assertEquals((beanOption[FaultCodeExport], bean[ServiceDesk].exportShards), (None, "0 Completed, 1 Completed"))
    }
  }

  @tailrec
  private def completed(processors: ProcessorManager, deadline: Deadline): Boolean =
    if (processors.status(WorkshopProcessor.FaultCodeExport).all(WorkerState.Completed)) true
    else if (deadline.isOverdue()) false
    else {
      Thread.sleep(10)
      completed(processors, deadline)
    }
}
