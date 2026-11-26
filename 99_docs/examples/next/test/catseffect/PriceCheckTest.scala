package com.example
package catseffect

import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite

final class PriceCheckTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[CatsEffectApplication.type]

  coilixTest("fetches the rates on the blocking executor and gives them as IO") {
    bean[ExchangeRates].rates(List("EUR", "USD")).map(rates => assertEquals(rates.keySet, Set("EUR", "USD")))
  }

  coilixTest("checks every product in the cache") {
    bean[PriceCheck].run(arguments).map(_ => assertEquals(bean[PriceCheck].checkedProducts, 2))
  }
}
