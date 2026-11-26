package com.example
package processing

import cats.effect.IO
import dev.coilix.processing.ProcessorManager
import dev.coilix.processing.WorkerState
import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

class ProcessingTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[ProcessingApplication.type]

  coilixTest("the shards of the export together export every customer") {
    within(bean[ProcessorManager].status(ShopProcessor.InvoiceExport).map(_.all(WorkerState.Completed))).map { completed =>
      assert(completed)
      assertEquals(bean[Invoices].exportedCustomers, bean[Invoices].customers.toSet)
    }
  }

  coilixTest("maintenance stops and starts the order watch through the manager") {
    val maintenance = bean[Maintenance]
    for {
      running <- within(maintenance.ordersRunning)
      paused  <- maintenance.pauseOrders
      _       <- maintenance.resumeOrders
      resumed <- within(maintenance.ordersRunning)
    } yield {
      assert(running && resumed)
      assert(paused.isComplete)
      assert(bean[Orders].pollCount > 0)
    }
  }

  coilixTest("stopping the order loading saves the batch it has already read") {
    for {
      read   <- within(IO(bean[OrderFeed].readCount == 3))
      result <- bean[ProcessorManager].stop(ShopProcessor.OrderLoading)
    } yield {
      assert(read && result.isComplete)
      assertEquals(bean[OrderStore].saved, List(bean[OrderFeed].incoming))
    }
  }

  private def within(condition: IO[Boolean]): IO[Boolean] =
    condition.ifM(IO.pure(true), IO.sleep(10.millis) *> within(condition)).timeoutTo(5.seconds, IO.pure(false))
}
