package com.example
package events

import dev.coilix.events.EventBus
import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class EventsTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[EventsApplication.type]

  coilixTest("synchronous listeners have handled the event when publish ends") {
    bean[Checkout].place("A-1", "ann", BigDecimal(120)).map { _ =>
      assertEquals((bean[SalesStatistics].total, bean[LoyaltyProgram].pointsOf("ann")), (BigDecimal(120), 12))
      assertEquals(recordedEvents[OrderPlaced].map(_.orderId), List("A-1"))
    }
  }

  coilixTest("an asynchronous listener handles the event on a fiber of its own") {
    for {
      _    <- bean[ExpressCheckout].place("A-2", "bob")
      idle <- bean[EventBus].awaitIdle(5.seconds)
    } yield {
      assert(idle)
      assertEquals(bean[ConfirmationMailer].sentMails, List("Order A-2 confirmed for bob"))
    }
  }

  coilixTest(
    "with synchronous events an asynchronous listener runs before publish ends",
    summon[CoilixTestSetup].withSynchronousEvents
  ) {
    bean[ExpressCheckout].place("A-3", "eve").map(_ =>
      assertEquals(bean[ConfirmationMailer].sentMails, List("Order A-3 confirmed for eve"))
    )
  }

  coilixTest("the shop opens when the application has started") {
    cats.effect.IO(assert(bean[ShopStatus].isOpen))
  }
}
