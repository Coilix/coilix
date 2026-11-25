package dev.coilix.processing

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.config.ConfigurationException
import dev.coilix.context.NoSuchBeanException
import dev.coilix.processing.Journal.awaitUntil
import dev.coilix.processing.fleet.Ticker
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixIOTestSetup.*
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class DefaultProcessorManagerTest extends CatsEffectSuite with CoilixContextOps {

  private def fleet(journal: Journal): CoilixTestSetup =
    CoilixTestSetup.forPackages("dev.coilix.processing.fleet").addBean(journal)

  private def running(id: ProcessorId, manager: ProcessorManager): IO[Boolean] =
    awaited(manager.status(id).map(_.all(WorkerState.Running)))

  private def awaited(condition: IO[Boolean]): IO[Boolean] =
    condition.ifM(IO.pure(true), IO.sleep(5.millis) *> awaited(condition)).timeoutTo(5.seconds, IO.pure(false))

  test("creates the processors with their dependencies without making them beans") {
    val journal = Journal()
    fleet(journal).withProperties("fleet.greeting" -> "hello").withIORuntime(munitIORuntime).useIO {
      awaitUntil(journal.contains("hello ticker#1")).map { greeted =>
        assertEquals(beanOption[Ticker], None)
        assertEquals(
          bean[ProcessorManager].processors.map(_.id),
          List(TestProcessor.Exporter, TestProcessor.Streamer, TestProcessor.Ticker)
        )
        assert(greeted, journal.all)
      }
    }
  }

  test("acquires, prepares and runs every shard of the replica on its own fiber") {
    val journal = Journal()
    fleet(journal).withIORuntime(munitIORuntime).useIO {
      running(TestProcessor.Ticker, bean[ProcessorManager]).map { started =>
        assert(started)
        assert(journal.all.indexOf("acquired ticker#0") < journal.all.indexOf("prepare ticker#0"), journal.all)
        assert(journal.contains("tick ticker#0"), journal.all)
      }
    }
  }

  test("stops a processor once, releases its shards and starts it again") {
    val journal = Journal()
    fleet(journal).withIORuntime(munitIORuntime).useIO {
      val manager = bean[ProcessorManager]
      for {
        _       <- running(TestProcessor.Ticker, manager)
        result  <- manager.stop(TestProcessor.Ticker)
        _       <- manager.stop(TestProcessor.Ticker)
        status  <- manager.status(TestProcessor.Ticker)
        _       <- IO(assertEquals(journal.count("released ticker#0 Stopped"), 1))
        _       <- manager.start(TestProcessor.Ticker)
        started <- awaitUntil(journal.count("acquired ticker#0") == 2)
      } yield {
        assertEquals(result, StopResult(TestProcessor.Ticker, List(0, 1), Nil))
        assert(status.all(WorkerState.Stopped))
        assert(started, journal.all)
      }
    }
  }

  test("releases every shard and closes the processors when the context closes") {
    val journal = Journal()
    fleet(journal).withIORuntime(munitIORuntime).useIO(running(TestProcessor.Streamer, bean[ProcessorManager])).map { started =>
      assert(started)
      assert(journal.contains("released ticker#1 Shutdown"), journal.all)
      assert(journal.contains("stream stopped streamer#0"), journal.all)
      assertEquals(journal.all.last, "closed ticker")
    }
  }

  test("completes a processor whose work ends") {
    val journal = Journal()
    fleet(journal).withIORuntime(munitIORuntime).useIO {
      for {
        completed <- awaited(bean[ProcessorManager].status(TestProcessor.Exporter).map(_.all(WorkerState.Completed)))
        released  <- awaitUntil(journal.contains("released exporter#0 Completed"))
      } yield {
        assert(completed)
        assertEquals(journal.startingWith("exported"), Vector("exported 256 slots of exporter#0"))
        assert(released, journal.all)
      }
    }
  }

  test("runs the shards that round-robin gives to this replica") {
    val journal = Journal()
    fleet(journal)
      .withProperties(
        "coilix.cluster.identity"         -> "static",
        "coilix.cluster.replicas"         -> "3",
        "coilix.cluster.replica-index"    -> "1",
        "coilix.processors.ticker.shards" -> "12"
      )
      .withIORuntime(munitIORuntime)
      .useIO {
        val manager = bean[ProcessorManager]
        val ticker  = manager.topology.processors.find(_.id == TestProcessor.Ticker).map(_.shards.map(_.shard))
        for {
          ticked <- awaitUntil(journal.contains("tick ticker#10"))
          error  <- manager.restart(TestProcessor.Ticker, 0).intercept[ShardNotOwnedException]
        } yield {
          assertEquals(ticker, Some(List(1, 4, 7, 10)))
          assert(ticked, journal.all)
          assertEquals(error.owners, List(0))
        }
      }
  }

  test("restarts one shard without touching the others") {
    val journal = Journal()
    fleet(journal).withIORuntime(munitIORuntime).useIO {
      val manager = bean[ProcessorManager]
      for {
        _         <- running(TestProcessor.Ticker, manager)
        _         <- manager.restart(TestProcessor.Ticker, 1)
        restarted <- awaitUntil(journal.count("prepare ticker#1") == 2)
      } yield {
        assert(restarted, journal.all)
        assertEquals((journal.count("prepare ticker#0"), journal.count("acquired ticker#1")), (1, 1))
      }
    }
  }

  test("restarts a failing shard with backoff and gives up after the allowed attempts") {
    val journal = Journal()
    CoilixTestSetup
      .forPackages("dev.coilix.processing.flaky")
      .addBean(journal)
      .withProperties(
        "coilix.processors.flaky.restart.backoff.initial" -> "5ms",
        "coilix.processors.flaky.restart.backoff.max"     -> "5ms",
        "coilix.processors.flaky.restart.max-attempts"    -> "2"
      )
      .withIORuntime(munitIORuntime)
      .useIO {
        val manager = bean[ProcessorManager]
        for {
          failed   <- awaited(manager.status(TestProcessor.Flaky).map(_.all(WorkerState.Failed)))
          shard    <- manager.status(TestProcessor.Flaky).map(_.shards.head)
          released <- awaitUntil(journal.contains("released flaky#0 Failed"))
          attempts  = journal.count("attempt flaky#0")
          _        <- manager.start(TestProcessor.Flaky)
          again    <- awaitUntil(journal.count("attempt flaky#0") == 6)
        } yield {
          assert(failed, journal.all)
          assertEquals((attempts, shard.restarts), (3, 2))
          assertEquals(
            shard.lastFailure.map(failure => (failure.stage, failure.message)),
            Some((FailureStage.Process, "the warehouse is offline"))
          )
          assert(released, journal.all)
          assert(again, journal.all)
        }
      }
  }

  test("leaves processors idle without autostart and stopped when disabled") {
    fleet(Journal())
      .withProperties("coilix.processors.autostart" -> "false", "coilix.processors.exporter.enabled" -> "false")
      .withIORuntime(munitIORuntime)
      .useIO {
        val manager = bean[ProcessorManager]
        (manager.status(TestProcessor.Ticker), manager.status(TestProcessor.Exporter)).mapN { (ticker, exporter) =>
          assert(ticker.all(WorkerState.Idle))
          assert(exporter.all(WorkerState.Stopped))
          assertEquals(manager.processors.find(_.id == TestProcessor.Exporter).map(_.settings.enabled), Some(false))
        }
      }
  }

  test("keeps a consistent state under operations from many fibers") {
    val journal = Journal()
    fleet(journal).withIORuntime(munitIORuntime).useIO {
      val manager = bean[ProcessorManager]
      for {
        _      <- List.range(0, 100).parTraverse_ { index =>
                    if (index % 2 == 0) manager.start(TestProcessor.Ticker) else manager.restart(TestProcessor.Ticker)
                  }
        _      <- manager.stop(TestProcessor.Ticker)
        status <- manager.status(TestProcessor.Ticker)
      } yield {
        assert(status.all(WorkerState.Stopped))
        assertEquals(journal.count("acquired ticker#0"), journal.count("released ticker#0 Stopped"))
      }
    }
  }

  test("rejects an identifier without a processor") {
    fleet(Journal()).withIORuntime(munitIORuntime).useIO {
      bean[ProcessorManager].start(TestProcessor.Twin).intercept[NoSuchProcessorException].void
    }
  }

  test("fails at startup when two processors have the same identifier") {
    startupFailure[InvalidProcessorException](CoilixTestSetup.forPackages("dev.coilix.processing.duplicate")).map { error =>
      assert(
        messages(error).exists(_.contains("FirstTwin and dev.coilix.processing.duplicate.SecondTwin have the same identifier twin"))
      )
    }
  }

  test("fails at startup when no engine runs a processor") {
    startupFailure[InvalidProcessorException](CoilixTestSetup.forPackages("dev.coilix.processing.orphan")).map { error =>
      assert(messages(error).exists(_.contains("Processor orphan (dev.coilix.processing.orphan.Orphan) has no engine")))
    }
  }

  test("fails at startup when a processor has more shards than slots") {
    startupFailure[ConfigurationException](fleet(Journal()).withProperties("coilix.processors.ticker.shards" -> "300")).map {
      error =>
        assert(messages(error).exists(_.contains("Processor ticker has 300 shards but only 256 slots")))
    }
  }

  test("fails at startup when a bean injects a processor") {
    startupFailure[NoSuchBeanException](CoilixTestSetup.forPackages("dev.coilix.processing.misuse")).map { error =>
      assert(messages(error).exists(_.contains("Processor")), messages(error))
    }
  }

  private def startupFailure[E <: Throwable](setup: CoilixTestSetup)(using scala.reflect.ClassTag[E]): IO[E] =
    setup.withIORuntime(munitIORuntime).useIO(IO.unit).intercept[E]

  private def messages(error: Throwable): List[String] =
    List.unfold(Option(error))(_.map(current => (String.valueOf(current.getMessage), Option(current.getCause))))
}
