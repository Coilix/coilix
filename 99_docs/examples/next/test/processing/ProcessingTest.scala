package com.example
package processing

import dev.coilix.processing.ProcessorManager
import dev.coilix.processing.WorkerState
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import scala.annotation.tailrec
import scala.concurrent.duration.Deadline
import scala.concurrent.duration.DurationInt

final class ProcessingTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[ProcessingApplication.type]

  test("the shards of the export together export every customer") {
    setup.use {
      assert(waitUntil(5.seconds.fromNow)(bean[ProcessorManager].status(ShopProcessor.InvoiceExport).all(WorkerState.Completed)))
      assertEquals(bean[Invoices].exportedCustomers, bean[Invoices].customers.toSet)
    }
  }

  test("maintenance stops and starts the order watch through the manager") {
    setup.use {
      val maintenance = bean[Maintenance]
      assert(waitUntil(5.seconds.fromNow)(maintenance.ordersRunning))
      assert(maintenance.pauseOrders().isComplete)
      maintenance.resumeOrders()
      assert(waitUntil(5.seconds.fromNow)(maintenance.ordersRunning))
    }
  }

  @tailrec
  private def waitUntil(deadline: Deadline)(condition: => Boolean): Boolean =
    if (condition) true
    else if (deadline.isOverdue()) false
    else {
      Thread.sleep(10)
      waitUntil(deadline)(condition)
    }
}
