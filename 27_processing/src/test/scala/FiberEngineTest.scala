package dev.coilix.processing

import cats.effect.IO
import dev.coilix.cluster.Member
import dev.coilix.processing.Journal.awaitUntil
import java.time.Instant
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class FiberEngineTest extends CatsEffectSuite {

  private val replica = Member(0, "replica-0", None, Instant.EPOCH, "test")

  private val engine = ResourceFunFixture(FiberEngine.resource)

  private def shard: IO[Shard] = Shard.create(TestProcessor.Ticker, SlotSet(0, 1, 4), replica)

  private def recording(journal: Journal): WorkerEvents =
    new WorkerEvents {
      override def prepared: IO[Unit] = IO(journal.add("prepared"))

      override def finished: IO[Unit] = IO(journal.add("finished"))

      override def crashed(stage: FailureStage, error: Throwable): IO[Unit] =
        IO(journal.add(s"crashed in $stage: ${error.getMessage}"))
    }

  private def processor(work: Shard => IO[Unit], preparation: Shard => IO[Unit] = _ => IO.unit): IOProcessor =
    new IOProcessor {
      override val id: ProcessorId = TestProcessor.Ticker

      override def prepare(shard: Shard): IO[Unit] = preparation(shard)

      override def run(shard: Shard): IO[Unit] = work(shard)
    }

  private def started(engine: FiberEngine, definition: IOProcessor, journal: Journal): IO[RunningWorker] =
    shard.flatMap(engine.start(definition, _, recording(journal)))

  engine.test("runs only IO processors") { engine =>
    assert(engine.supports(processor(_ => IO.unit)))
    assert(!engine.supports(new ProcessorDefinition { override val id: ProcessorId = TestProcessor.Orphan }))
  }

  engine.test("reports a failure of prepare without running the work") { engine =>
    val journal = Journal()
    val failing = processor(_ => IO(journal.add("ran")), _ => IO.raiseError(IllegalStateException("no offsets")))
    started(engine, failing, journal) *>
      awaitUntil(journal.all == Vector("crashed in Prepare: no offsets")).map(assert(_, journal.all))
  }

  engine.test("reports a failure of the work after a successful prepare") { engine =>
    val journal = Journal()
    started(engine, processor(_ => IO.raiseError(IllegalArgumentException("bad record"))), journal) *>
      awaitUntil(journal.all == Vector("prepared", "crashed in Process: bad record")).map(assert(_, journal.all))
  }

  engine.test("lets work that watches the stop signal finish its step") { engine =>
    val journal = Journal()
    val polite  = processor(shard => shard.stopped *> IO(journal.add("step finished")))
    for {
      worker  <- started(engine, polite, journal)
      _       <- awaitUntil(journal.contains("prepared"))
      outcome <- worker.stop(1.second)
      ended   <- awaitUntil(journal.all == Vector("prepared", "step finished", "finished"))
    } yield {
      assertEquals(outcome, StopOutcome.Stopped)
      assert(ended, journal.all)
    }
  }

  engine.test("cancels work that does not watch the stop signal after half of the timeout") { engine =>
    val journal = Journal()
    val deaf    = processor(_ => IO.never.onCancel(IO(journal.add("cancelled"))))
    for {
      worker  <- started(engine, deaf, journal)
      _       <- awaitUntil(journal.contains("prepared"))
      outcome <- worker.stop(200.millis)
    } yield {
      assertEquals(outcome, StopOutcome.Stopped)
      assertEquals(journal.all, Vector("prepared", "cancelled"))
    }
  }

  engine.test("abandons work that cannot be cancelled") { engine =>
    val journal = Journal()
    val stuck   = processor(_ => IO.uncancelable(_ => IO.sleep(2.seconds)))
    for {
      worker  <- started(engine, stuck, journal)
      _       <- awaitUntil(journal.contains("prepared"))
      outcome <- worker.stop(100.millis)
    } yield assertEquals(outcome, StopOutcome.Abandoned)
  }

  test("cancels running work when the engine closes") {
    val journal = Journal()
    val endless = processor(_ => IO.never.onCancel(IO(journal.add("cancelled"))))
    FiberEngine.resource
      .use(engine => started(engine, endless, journal) *> awaitUntil(journal.contains("prepared")))
      .map(_ => assertEquals(journal.all, Vector("prepared", "cancelled")))
  }
}
