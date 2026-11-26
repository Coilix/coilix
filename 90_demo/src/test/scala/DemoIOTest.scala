package dev.coilix.demo

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.demo.vehicle.Vin
import dev.coilix.demo.workshop.CheckInLog
import dev.coilix.demo.workshop.FaultCodeExport
import dev.coilix.demo.workshop.FaultLedger
import dev.coilix.demo.workshop.ModuleCensus
import dev.coilix.demo.workshop.PortalSession
import dev.coilix.demo.workshop.RecallBoard
import dev.coilix.demo.workshop.RecallBriefing
import dev.coilix.demo.workshop.RecallCheck
import dev.coilix.demo.workshop.ServiceDesk
import dev.coilix.demo.workshop.TroubleshootingDesk
import dev.coilix.demo.workshop.WorkshopProcessor
import dev.coilix.logging.LogLevel
import dev.coilix.logging.LogLevels
import dev.coilix.processing.ProcessorManager
import dev.coilix.processing.WorkerState
import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixIOTestSetup.*
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class DemoIOTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[DemoApplication.type]

  coilixTest("the morning briefing reads the pinned recall notice, written as an IO test") {
    bean[RecallBriefing].run(arguments).map(_ =>
      assertEquals((bean[RecallBriefing].isDone, bean[RecallBoard].pinned.isDefined), (true, true))
    )
  }

  coilixTest("checks recalls of the whole fleet with IO, waiting for the portal on the blocking executor") {
    bean[RecallCheck].openRecalls.map(lines =>
      assertEquals(
        (lines.map(_.takeWhile(_ != ':')), lines.forall(_.contains("coilix-blocking-"))),
        (List("WVWZZZ1KZ6W000001", "TMBJJ7NE8J0000002"), true)
      )
    )
  }

  test("logs in to the portal with a Resource bean, downloads the bulletin with an IO bean and logs out when closing") {
    summon[CoilixTestSetup].resource.use(portalCheck).map(result =>
      assertEquals(
        (result._1.isOpen, result._2.forall(_.contains("in bulletin 2026-11")), result._1.url),
        (false, true, "https://oem-portal.example.com/workshop-keys")
      )
    )
  }

  coilixTest("counts the modules of the fleet with an fs2 stream") {
    bean[ModuleCensus].count.map(counts => assertEquals(counts, Map("engine" -> 2, "abs" -> 2, "immobilizer" -> 1)))
  }

  coilixTest("exports the fault codes of the fleet with a background processor on two shards") {
    val processors = bean[ProcessorManager]
    for {
      completed <- completed(processors)
      shards    <- bean[ServiceDesk].exportShards
    } yield {
      assert(completed)
      assertEquals(bean[FaultLedger].faults, Map(vin -> List("P0301", "P0420", "C0035"), Vin("TMBJJ7NE8J0000002") -> Nil))
      assertEquals(
        processors.topology.processors.map(processor => (processor.id, processor.shards.map(_.shard))),
        List((WorkshopProcessor.FaultCodeExport, List(0, 1)), (WorkshopProcessor.VehicleCheckIn, List(0)))
      )
      assertEquals((beanOption[FaultCodeExport], shards), (None, "0 Completed, 1 Completed"))
    }
  }

  coilixTest("saves the batch of checked-in vehicles waiting in the stream when the processor stops") {
    val checkIns = bean[CheckInLog]
    for {
      arrived <- awaitArrivals(checkIns)
      before  <- IO(checkIns.batches)
      result  <- bean[ProcessorManager].stop(WorkshopProcessor.VehicleCheckIn)
    } yield {
      assert(arrived && result.isComplete, s"$result after ${checkIns.arrivals}")
      assertEquals((before, checkIns.batches), (Nil, List(List(vin, Vin("TMBJJ7NE8J0000002")))))
    }
  }

  coilixTest("turns the logs of the processors up and down at runtime, and reads the levels of application.yaml") {
    val desk = bean[TroubleshootingDesk]
    for {
      during     <- desk.investigateProcessors
      after      <- desk.closeInvestigation
      reflection <- bean[LogLevels].levelOf("org.reflections.Reflections")
    } yield assertEquals((during, after, reflection), (LogLevel.Debug, LogLevel.Info, LogLevel.Warn))
  }

  coilixTest("the workshop runs its effects on the runtime of the test") {
    IO(assert(bean[IORuntime] eq munitIORuntime))
  }

  private val vin = Vin("WVWZZZ1KZ6W000001")

  private def awaitArrivals(checkIns: CheckInLog): IO[Boolean] =
    IO(checkIns.arrivals.size == 2)
      .flatMap(done => if (done) IO.pure(true) else IO.sleep(10.millis) *> awaitArrivals(checkIns))
      .timeoutTo(5.seconds, IO.pure(false))

  private def completed(processors: ProcessorManager): IO[Boolean] =
    processors
      .status(WorkshopProcessor.FaultCodeExport)
      .map(_.all(WorkerState.Completed))
      .flatMap(done => if (done) IO.pure(true) else IO.sleep(10.millis) *> completed(processors))
      .timeoutTo(5.seconds, IO.pure(false))

  private def portalCheck(context: ConfigurableApplicationContext): IO[(PortalSession, List[String])] =
    context.getBean[RecallCheck].openRecalls.map(lines => (context.getBean[PortalSession], lines))
}
