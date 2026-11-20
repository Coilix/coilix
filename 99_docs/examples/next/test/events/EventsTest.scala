package com.example
package events

import dev.coilix.events.EventBus
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import scala.concurrent.duration.DurationInt

final class EventsTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[EventsApplication.type]

  test("synchronous listeners have handled the event when publish returns") {
    setup.use {
      bean[Checkout].place("A-1", "ann", BigDecimal(120))
      assertEquals((bean[SalesStatistics].total, bean[LoyaltyProgram].pointsOf("ann")), (BigDecimal(120), 12))
    }
  }

  test("an asynchronous listener handles the event on the task executor") {
    setup.use {
      bean[ExpressCheckout].place("A-2", "bob")
      assert(bean[EventBus].awaitIdle(5.seconds))
      assertEquals(bean[ConfirmationMailer].sentMails, List("Order A-2 confirmed for bob"))
    }
  }
}
