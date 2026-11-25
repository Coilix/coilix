package dev.coilix.demo

import cats.effect.IO
import dev.coilix.demo.workshop.JobEstimates
import dev.coilix.demo.workshop.LabourRates
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.catseffect.CoilixIOSuite
import munit.CatsEffectSuite

final class DemoTaglessTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[DemoApplication.type]

  coilixTest("estimates a job with tagless final components whose F the container binds to IO") {
    bean[JobEstimates[IO]].estimate("brakes", 3).map(estimate => assertEquals(estimate, "brakes: 3 h x 90 = 270"))
  }

  coilixTest("the rates fall back to the general rate for other systems") {
    bean[LabourRates[IO]].hourlyRate("lights").map(rate => assertEquals(rate, 80))
  }
}
