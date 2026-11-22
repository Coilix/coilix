package com.example
package scopes

import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup

final class ScopesTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[ScopesApplication.type]

  test("every cart is a new one") {
    setup.use {
      val first = bean[Checkout].newCart()
      first.add("lamp")
      assertEquals(bean[Checkout].newCart().contents, Nil)
    }
  }

  test("the catalog is loaded on the first quote, once") {
    setup.use {
      val before = ProductCatalog.loads.get
      val quotes = List(bean[PriceDesk].quote("lamp"), bean[PriceDesk].quote("desk"))
      assertEquals((quotes, ProductCatalog.loads.get - before), (List(BigDecimal(4), BigDecimal(4)), 1))
    }
  }
}
