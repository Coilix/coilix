package dev.coilix.demo

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.demo.workshop.ModuleCensus
import dev.coilix.demo.workshop.RecallBoard
import dev.coilix.demo.workshop.RecallBriefing
import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite

final class DemoIOTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[DemoApplication.type]

  coilixTest("the morning briefing reads the pinned recall notice, written as an IO test") {
    bean[RecallBriefing].run(arguments).map(_ =>
      assertEquals((bean[RecallBriefing].isDone, bean[RecallBoard].pinned.isDefined), (true, true))
    )
  }

  coilixTest("counts the modules of the fleet with an fs2 stream") {
    bean[ModuleCensus].count.map(counts => assertEquals(counts, Map("engine" -> 2, "abs" -> 2, "immobilizer" -> 1)))
  }

  coilixTest("the workshop runs its effects on the runtime of the test") {
    IO(assert(bean[IORuntime] eq munitIORuntime))
  }
}
