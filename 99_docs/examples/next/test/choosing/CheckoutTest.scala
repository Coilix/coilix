package com.example
package choosing

import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup

final class CheckoutTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[ChoosingApplication.type]

  test("a plain parameter gets the primary bean") {
    setup.use {
      assertEquals(bean[Checkout].payNow(10), "card payment of 10")
    }
  }

  test("a qualified parameter gets the bean with that name") {
    setup.use {
      assertEquals(bean[Checkout].payByInvoice(10), "bank transfer of 10")
    }
  }

  test("a list parameter gets every bean") {
    setup.use {
      assertEquals(bean[Checkout].gatewayCount, 2)
    }
  }
}
