package dev.coilix.processing

import dev.coilix.processing.Journal.waitUntil
import dev.coilix.processing.stateless.Order
import dev.coilix.processing.stateless.Orders
import dev.coilix.processing.stateless.Shipped
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class StatelessProcessorTest extends FunSuite with CoilixContextOps {
  private val shop = CoilixTestSetup.forPackages("dev.coilix.processing.stateless")

  private val orders = (1 to 20).toList.map(number => Order(number, s"customer-${number % 7}"))

  test("processes every element once on the shards of its slots and counts it in the statistics") {
    shop.use {
      bean[Orders].publish(orders*)
      assert(waitUntil(bean[Orders].pending == 0), bean[Orders].pending)
      val statistics = bean[ProcessorManager].statistics(TestProcessor.Shipping)
      assertEquals(bean[Shipped].emitted.sorted, (1 to 20).toList)
      assertEquals((statistics.processed, statistics.failed, statistics.lag, statistics.shards.size), (20L, 0L, 0L, 2))
      assert(statistics.shards.forall(_.metrics.processed > 0), statistics)
      assert(statistics.shards.forall(_.metrics.lastProcessed.isDefined))
    }
  }

  test("keeps the order of the elements of one key") {
    shop.use {
      val sameCustomer = (1 to 30).toList.map(number => Order(number, "customer-1"))
      bean[Orders].publish(sameCustomer*)
      assert(waitUntil(bean[Shipped].emitted.size == 30))
      assertEquals(bean[Shipped].emitted, (1 to 30).toList)
    }
  }

  test("restarts the worker after a failed element, which comes again, and publishes the failure and the states") {
    shop.withProperties("shipping.jammed" -> "7", "coilix.processors.shipping.restart.backoff.initial" -> "10ms").use {
      bean[Orders].publish(orders*)
      assert(waitUntil(bean[Orders].pending == 0, 10.seconds), bean[Orders].pending)
      val statistics = bean[ProcessorManager].statistics(TestProcessor.Shipping)
      assertEquals((bean[Shipped].emitted.sorted, statistics.processed, statistics.failed), ((1 to 20).toList, 20L, 1L))
      assert(awaitEvents())
      val failures   = recordedEvents[ProcessorEvent.ShardFailed]
      assertEquals(failures.map(_.failure.errorClass), List(classOf[ElementFailedException].getName))
      assert(failures.head.failure.message.contains("delivery 1"), failures.head.failure.message)
      val shard      = failures.head.shard
      val states     = recordedEvents[ProcessorEvent.ShardStateChanged].filter(_.shard == shard).map(_.current)
      assertEquals(
        states.dropWhile(_ != WorkerState.Backoff).take(3),
        List(WorkerState.Backoff, WorkerState.Preparing, WorkerState.Running)
      )
    }
  }

  test("a worker whose source has another number of slots than the processor fails at start with the key to set") {
    CoilixTestSetup.forPackages("dev.coilix.processing.misfit").use {
      assert(awaitEvents())
      assert(waitUntil(recordedEvents[ProcessorEvent.ShardFailed].nonEmpty))
      val failure = recordedEvents[ProcessorEvent.ShardFailed].head.failure
      assertEquals(failure.errorClass, classOf[InvalidProcessorException].getName)
      assert(failure.message.contains("coilix.processors.misfit.slots = 16"), failure.message)
    }
  }
}
