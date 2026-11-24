package dev.coilix.processing

import dev.coilix.cluster.Member
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.processing.Journal.waitUntil
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import munit.FunSuite
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt

final class VirtualThreadEngineTest extends FunSuite {

  private val engine  = VirtualThreadEngine(FatalErrorHandler.logOnly)
  private val replica = Member(0, "replica-0", None, Instant.EPOCH, "test")

  private def shard: Shard = Shard(TestProcessor.Ticker, SlotSet(0, 1, 4), replica)

  private def recording(journal: Journal): WorkerEvents =
    new WorkerEvents {
      override def prepared(): Unit = journal.add("prepared")

      override def finished(): Unit = journal.add("finished")

      override def crashed(stage: FailureStage, error: Throwable): Unit = journal.add(s"crashed in $stage: ${error.getMessage}")
    }

  private def blocking(work: Shard => Unit, preparation: Shard => Unit = _ => ()): BlockingProcessor =
    new BlockingProcessor {
      override val id: ProcessorId = TestProcessor.Ticker

      override def prepare(shard: Shard): Unit = preparation(shard)

      override def run(shard: Shard): Unit = work(shard)
    }

  test("runs only blocking and custom processors") {
    assert(engine.supports(blocking(_ => ())))
    assert(!engine.supports(new ProcessorDefinition { override val id: ProcessorId = TestProcessor.Orphan }))
  }

  test("reports a failure of prepare without running the work") {
    val journal = Journal()
    engine.start(blocking(_ => journal.add("ran"), _ => throw IllegalStateException("no offsets")), shard, recording(journal))
    assert(waitUntil(journal.all == Vector("crashed in Prepare: no offsets")), journal.all)
  }

  test("reports a failure of the work after a successful prepare") {
    val journal = Journal()
    engine.start(blocking(_ => throw IllegalArgumentException("bad record")), shard, recording(journal))
    assert(waitUntil(journal.all == Vector("prepared", "crashed in Process: bad record")), journal.all)
  }

  test("interrupts work that does not watch the stop flag and counts it as a normal end") {
    val journal = Journal()
    val started = CountDownLatch(1)
    val worker  = engine.start(blocking { _ => started.countDown(); Thread.sleep(60000) }, shard, recording(journal))
    started.await()
    assertEquals(worker.stop(200.millis), StopOutcome.Stopped)
    assert(waitUntil(journal.all == Vector("prepared", "finished")), journal.all)
  }

  test("abandons work that ignores both the stop flag and interruption") {
    val release = AtomicBoolean(false)
    val started = CountDownLatch(1)
    val worker  = engine.start(blocking { _ => started.countDown(); spin(release) }, shard, recording(Journal()))
    started.await()
    assertEquals(worker.stop(100.millis), StopOutcome.Abandoned)
    release.set(true)
  }

  @tailrec
  private def spin(release: AtomicBoolean): Unit =
    if (!release.get) {
      Thread.onSpinWait()
      spin(release)
    }
}
