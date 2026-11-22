package com.example
package tasks

import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class TasksTest extends munit.FunSuite with CoilixContextOps {
  private val mailSender = FlakyMailSender()
  private val setup      = CoilixTestSetup.forApplication[TasksApplication.type].addBean[MailSender](mailSender)

  test("a computation runs on the compute pool") {
    setup.use {
      assert(Await.result(bean[ThumbnailService].render("cat.png"), 5.seconds).contains("compute-"))
    }
  }

  test("a blocking call runs on the blocking executor") {
    setup.use {
      val stored = Await.result(bean[InvoiceArchive].store("INV-7", "total 120"), 5.seconds)
      assertEquals(java.nio.file.Files.readString(stored), "total 120")
    }
  }

  test("a blocking @Async method runs on the blocking executor and the bean is injected through its trait") {
    setup.use {
      assert(Await.result(bean[InvoiceMailer].send("INV-7"), 5.seconds).startsWith("INV-7 sent from coilix-blocking-"))
    }
  }

  test("a task runs with the tenant of the thread that submitted it") {
    setup.use {
      val report = CurrentTenant.withTenant(Some("acme"))(bean[TenantReport].build())
      assertEquals(Await.result(report, 5.seconds), "report for acme")
    }
  }

  test("a @Scheduled method runs on its own") {
    setup.use {
      val refresher = bean[PriceRefresher]
      assert(LazyList.range(0, 100).exists(_ => refresher.refreshes >= 2 || { Thread.sleep(50); false }))
    }
  }

  test("the scheduler keeps refreshing until the context closes") {
    setup.use {
      val refresher = bean[StockRefresher]
      assert(LazyList.range(0, 100).exists(_ => refresher.refreshes >= 2 || { Thread.sleep(50); false }))
    }
  }

  test("a failed mail is sent again after the wait of the retry policy") {
    setup.use {
      bean[RetryingMailer].send("ann@example.com", "Your order has shipped")
      assert(mailSender.delivered.await(5, TimeUnit.SECONDS))
    }
  }
}
