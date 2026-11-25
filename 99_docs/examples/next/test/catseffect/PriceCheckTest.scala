package com.example
package catseffect

import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite

final class PriceCheckTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[CatsEffectApplication.type]

  coilixTest("checks every product in the cache") {
    bean[PriceCheck].run(arguments).map(_ => assertEquals(bean[PriceCheck].checkedProducts, 2))
  }
}
