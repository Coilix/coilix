package com.example
package generics

import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup

final class PriceReportTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[GenericsApplication.type]

  test("each dependency gets the source with its own type argument") {
    setup.use {
      assertEquals(bean[PriceReport].describe("tea"), "tea: 12 (earlier 10, 12)")
    }
  }

  test("beans are looked up by their full type") {
    setup.use {
      assertEquals(bean[PriceSource[Option]].price("tea"), Some(BigDecimal(12)))
    }
  }

  test("a test replaces only the source with the given type argument") {
    val fixed: PriceSource[Option] = _ => Some(BigDecimal(9))
    setup.replaceBean[PriceSource[Option]](fixed).use {
      assertEquals(bean[PriceReport].describe("tea"), "tea: 9 (earlier 10, 12)")
    }
  }
}
