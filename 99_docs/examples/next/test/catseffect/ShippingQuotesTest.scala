package com.example
package catseffect

import cats.effect.IO
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.catseffect.CoilixIOSuite
import munit.CatsEffectSuite

final class ShippingQuotesTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[CatsEffectApplication.type]

  coilixTest("tagless final components are beans with F bound to IO") {
    bean[ShippingQuotes[IO]].quote("DE", 2).map(price => assertEquals(price, 50))
  }
}
