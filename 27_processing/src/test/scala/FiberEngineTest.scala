package dev.coilix.processing

import cats.effect.IO
import dev.coilix.cluster.Member
import dev.coilix.processing.Journal.awaitUntil
import fs2.Pipe
import fs2.Stream
import java.time.Instant
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

final class FiberEngineTest extends CatsEffectSuite {

  private val replica = Member(0, "replica-0", None, Instant.EPOCH, "test")

  private val engine = ResourceFunFixture(FiberEngine.resource)

  private def shard: IO[Shard] = Shard.create(TestProcessor.Ticker, SlotSet(0, 1, 4), replica)

  private def drainingShard(drainTimeout: FiniteDuration): IO[Shard] =
    Shard.create(TestProcessor.Ticker, SlotSet(0, 1, 4), replica, Some(drainTimeout))

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

  private def streaming(elements: Stream[IO, Int], through: Pipe[IO, Int, Unit]): StreamProcessor[Int] =
    new StreamProcessor[Int] {
      override val id: ProcessorId = TestProcessor.Ticker

      override def source(shard: Shard): Stream[IO, Int] = elements

      override def flow(shard: Shard): Pipe[IO, Int, Unit] = through
    }

  private def started(engine: FiberEngine, definition: ProcessorDefinition, journal: Journal): IO[RunningWorker] =
    shard.flatMap(engine.start(definition, _, recording(journal)))

  engine.test("runs IO and stream processors") { engine =>
    assert(engine.supports(processor(_ => IO.unit)))
    assert(engine.supports(streaming(Stream.empty, _.drain)))
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

  engine.test("finishes the elements a stream processor has already read before it stops") { engine =>
    val journal = Journal()
    val batched = streaming(
      Stream.emits(1 to 5) ++ Stream.never[IO],
      _.evalTap(element => IO(journal.add(s"read $element"))).chunkN(10).evalMap(batch => IO(journal.add(s"saved ${batch.toList}")))
    )
    for {
      worker  <- started(engine, batched, journal)
      _       <- awaitUntil(journal.contains("read 5"))
      outcome <- worker.stop(1.second)
      ended   <- awaitUntil(journal.contains("finished"))
    } yield {
      assertEquals(outcome, StopOutcome.Stopped)
      assert(ended, journal.all)
      assertEquals(journal.startingWith("saved"), Vector("saved List(1, 2, 3, 4, 5)"))
    }
  }

  engine.test("finishes an element the flow is processing when the stop signal comes") { engine =>
    val journal  = Journal()
    val stopping = new StreamProcessor[Int] {
      override val id: ProcessorId = TestProcessor.Ticker

      override def source(shard: Shard): Stream[IO, Int] = Stream.emits(1 to 5) ++ Stream.never[IO]

      override def flow(shard: Shard): Pipe[IO, Int, Unit] =
        _.evalTap(element => IO.whenA(element == 5)(shard.requestStop *> IO.cede))
          .chunkN(10)
          .evalMap(batch => IO(journal.add(s"saved ${batch.toList}")))
    }
    (1 to 200).toList.foldLeft(IO.unit) { (previous, _) =>
      previous *> shard.flatMap(stopping.run).timeout(5.seconds) *> IO(journal.startingWith("saved")).map { saved =>
        assertEquals(saved.lastOption, Some("saved List(1, 2, 3, 4, 5)"))
      }
    }
  }

  engine.test("completes a stream processor whose source ends") { engine =>
    val journal = Journal()
    val finite  = streaming(Stream.emits(1 to 3), _.evalMap(element => IO(journal.add(s"saved $element"))))
    started(engine, finite, journal) *>
      awaitUntil(journal.all == Vector("prepared", "saved 1", "saved 2", "saved 3", "finished")).map(assert(_, journal.all))
  }

  engine.test("cancels a stream processor whose flow does not end within the drain timeout") { engine =>
    val journal = Journal()
    val hanging =
      streaming(Stream.emit(1), _.evalMap(_ => IO(journal.add("taken")) *> IO.never[Unit].onCancel(IO(journal.add("cancelled")))))
    for {
      worker  <- drainingShard(100.millis).flatMap(engine.start(hanging, _, recording(journal)))
      _       <- awaitUntil(journal.contains("taken"))
      outcome <- worker.stop(10.seconds).timed
    } yield {
      assertEquals((outcome._2, journal.contains("cancelled")), (StopOutcome.Stopped, true))
      assert(outcome._1 < 5.seconds, s"stopped after ${outcome._1}")
    }
  }

  test("cancels running work when the engine closes") {
    val journal = Journal()
    val endless = processor(_ => IO.never.onCancel(IO(journal.add("cancelled"))))
    FiberEngine.resource
      .use(engine => started(engine, endless, journal) *> awaitUntil(journal.contains("prepared")))
      .map(_ => assertEquals(journal.all, Vector("prepared", "cancelled")))
  }
}
