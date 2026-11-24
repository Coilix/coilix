package dev.coilix.processing

import dev.coilix.config.ConfigurationException
import dev.coilix.context.BeanCreationException
import dev.coilix.context.NoSuchBeanException
import dev.coilix.processing.Journal.waitUntil
import dev.coilix.processing.fleet.Ticker
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import munit.FunSuite
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

final class DefaultProcessorManagerTest extends FunSuite with CoilixContextOps {

  private def fleet(journal: Journal): CoilixTestSetup =
    CoilixTestSetup.forPackages("dev.coilix.processing.fleet").addBean(journal)

  test("creates the processors with their dependencies without making them beans") {
    val journal = Journal()
    fleet(journal).withProperties("fleet.greeting" -> "hello").use {
      assertEquals(beanOption[Ticker], None)
      assertEquals(
        bean[ProcessorManager].processors.map(_.id),
        List(TestProcessor.Exporter, TestProcessor.Streamer, TestProcessor.Ticker)
      )
      assert(waitUntil(journal.contains("hello ticker#1 on coilix-processor-ticker-1")), journal.all)
    }
  }

  test("acquires, prepares and runs every shard of the replica on its own thread") {
    val journal = Journal()
    fleet(journal).use {
      assert(waitUntil(bean[ProcessorManager].status(TestProcessor.Ticker).all(WorkerState.Running)))
      assert(journal.all.indexOf("acquired ticker#0") < journal.all.indexOf("prepare ticker#0"), journal.all)
      assert(journal.contains("tick ticker#0 on coilix-processor-ticker-0"), journal.all)
    }
  }

  test("stops a processor once, releases its shards and starts it again") {
    val journal = Journal()
    fleet(journal).use {
      val manager = bean[ProcessorManager]
      assert(waitUntil(manager.status(TestProcessor.Ticker).all(WorkerState.Running)))
      assertEquals(manager.stop(TestProcessor.Ticker), StopResult(TestProcessor.Ticker, List(0, 1), Nil))
      manager.stop(TestProcessor.Ticker)
      assert(manager.status(TestProcessor.Ticker).all(WorkerState.Stopped))
      assertEquals(journal.count("released ticker#0 Stopped"), 1)
      manager.start(TestProcessor.Ticker)
      assert(waitUntil(journal.count("acquired ticker#0") == 2), journal.all)
    }
  }

  test("releases every shard and closes the processors when the context closes") {
    val journal = Journal()
    fleet(journal).use {
      assert(waitUntil(bean[ProcessorManager].status(TestProcessor.Streamer).all(WorkerState.Running)))
    }
    assert(journal.contains("released ticker#1 Shutdown"), journal.all)
    assert(journal.contains("stream stopped streamer#0"), journal.all)
    assertEquals(journal.all.last, "closed ticker")
  }

  test("completes a processor whose work ends") {
    val journal = Journal()
    fleet(journal).use {
      assert(waitUntil(bean[ProcessorManager].status(TestProcessor.Exporter).all(WorkerState.Completed)))
      assertEquals(journal.startingWith("exported"), Vector("exported 256 slots of exporter#0"))
      assert(journal.contains("released exporter#0 Completed"), journal.all)
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
      .use {
        val manager = bean[ProcessorManager]
        val ticker  = manager.topology.processors.find(_.id == TestProcessor.Ticker).map(_.shards.map(_.shard))
        assertEquals(ticker, Some(List(1, 4, 7, 10)))
        assert(waitUntil(journal.contains("tick ticker#10 on coilix-processor-ticker-10")), journal.all)
        val error   = intercept[ShardNotOwnedException](manager.restart(TestProcessor.Ticker, 0))
        assertEquals(error.owners, List(0))
      }
  }

  test("restarts one shard without touching the others") {
    val journal = Journal()
    fleet(journal).use {
      val manager = bean[ProcessorManager]
      assert(waitUntil(manager.status(TestProcessor.Ticker).all(WorkerState.Running)))
      manager.restart(TestProcessor.Ticker, 1)
      assert(waitUntil(journal.count("prepare ticker#1") == 2), journal.all)
      assertEquals((journal.count("prepare ticker#0"), journal.count("acquired ticker#1")), (1, 1))
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
      .use {
        val manager = bean[ProcessorManager]
        assert(waitUntil(manager.status(TestProcessor.Flaky).all(WorkerState.Failed)), journal.all)
        val shard   = manager.status(TestProcessor.Flaky).shards.head
        assertEquals((journal.count("attempt flaky#0"), shard.restarts), (3, 2))
        assertEquals(
          shard.lastFailure.map(failure => (failure.stage, failure.message)),
          Some((FailureStage.Process, "the warehouse is offline"))
        )
        assert(journal.contains("released flaky#0 Failed"))
        manager.start(TestProcessor.Flaky)
        assert(waitUntil(journal.count("attempt flaky#0") == 6), journal.all)
      }
  }

  test("leaves processors idle without autostart and stopped when disabled") {
    val journal = Journal()
    fleet(journal)
      .withProperties("coilix.processors.autostart" -> "false", "coilix.processors.exporter.enabled" -> "false")
      .use {
        val manager = bean[ProcessorManager]
        assert(manager.status(TestProcessor.Ticker).all(WorkerState.Idle))
        assert(manager.status(TestProcessor.Exporter).all(WorkerState.Stopped))
        assertEquals(manager.processors.find(_.id == TestProcessor.Exporter).map(_.settings.enabled), Some(false))
      }
  }

  test("keeps a consistent state under operations from many threads") {
    val journal = Journal()
    fleet(journal).use {
      val manager            = bean[ProcessorManager]
      given ExecutionContext = ExecutionContext.global
      val calls              = Future.traverse(List.range(0, 100)) { index =>
        Future(if (index % 2 == 0) manager.start(TestProcessor.Ticker) else manager.restart(TestProcessor.Ticker))
      }
      Await.result(calls, 30.seconds)
      manager.stop(TestProcessor.Ticker)
      assert(manager.status(TestProcessor.Ticker).all(WorkerState.Stopped))
      assertEquals(journal.count("acquired ticker#0"), journal.count("released ticker#0 Stopped"))
    }
  }

  test("rejects an identifier without a processor") {
    fleet(Journal()).use {
      intercept[NoSuchProcessorException](bean[ProcessorManager].start(TestProcessor.Twin))
    }
  }

  test("fails at startup when two processors have the same identifier") {
    val error = intercept[BeanCreationException](CoilixTestSetup.forPackages("dev.coilix.processing.duplicate").use(()))
    assert(error.getCause.isInstanceOf[InvalidProcessorException], error.getCause)
    assert(
      messages(error).exists(_.contains("FirstTwin and dev.coilix.processing.duplicate.SecondTwin have the same identifier twin"))
    )
  }

  test("fails at startup when no engine runs a processor") {
    val error = intercept[BeanCreationException](CoilixTestSetup.forPackages("dev.coilix.processing.orphan").use(()))
    assert(error.getCause.isInstanceOf[InvalidProcessorException], error.getCause)
    assert(messages(error).exists(_.contains("Processor orphan (dev.coilix.processing.orphan.Orphan) has no engine")))
  }

  test("fails at startup when a processor has more shards than slots") {
    val error = intercept[BeanCreationException](
      fleet(Journal()).withProperties("coilix.processors.ticker.shards" -> "300").use(())
    )
    assert(error.getCause.isInstanceOf[ConfigurationException], error.getCause)
    assert(messages(error).exists(_.contains("Processor ticker has 300 shards but only 256 slots")))
  }

  test("fails at startup when a bean injects a processor") {
    val error = intercept[NoSuchBeanException](CoilixTestSetup.forPackages("dev.coilix.processing.misuse").use(()))
    assert(messages(error).exists(_.contains("Processor")), messages(error))
  }

  private def messages(error: Throwable): List[String] =
    List.unfold(Option(error))(_.map(current => (String.valueOf(current.getMessage), Option(current.getCause))))
}
