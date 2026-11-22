package com.example
package events

import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup

final class EventsTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[EventsApplication.type]

  test("synchronous listeners have handled the event when publish returns") {
    setup.use {
      bean[Checkout].place("A-1", "ann", BigDecimal(120))
      assertEquals((bean[SalesStatistics].total, bean[LoyaltyProgram].pointsOf("ann")), (BigDecimal(120), 12))
      assertEquals(recordedEvents[OrderPlaced].map(_.orderId), List("A-1"))
    }
  }

  test("an asynchronous listener handles the event on the task executor") {
    setup.use {
      bean[ExpressCheckout].place("A-2", "bob")
      assert(awaitEvents())
      assertEquals(bean[ConfirmationMailer].sentMails, List("Order A-2 confirmed for bob"))
    }
  }

  test("with synchronous events an asynchronous listener runs before publish returns") {
    setup.withSynchronousEvents.use {
      bean[ExpressCheckout].place("A-3", "eve")
      assertEquals(bean[ConfirmationMailer].sentMails, List("Order A-3 confirmed for eve"))
    }
  }

  test("the shop opens when the application has started") {
    setup.use(assert(bean[ShopStatus].isOpen))
  }
}
