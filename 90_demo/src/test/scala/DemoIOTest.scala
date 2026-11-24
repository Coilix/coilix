package dev.coilix.demo

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.demo.workshop.RecallBoard
import dev.coilix.demo.workshop.RecallBriefing
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.catseffect.CoilixIOSuite
import munit.CatsEffectSuite

final class DemoIOTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forApplication[DemoApplication.type]

  coilixTest("the morning briefing reads the pinned recall notice, written as an IO test") {
    bean[RecallBriefing].run(arguments).map(_ =>
      assertEquals((bean[RecallBriefing].isDone, bean[RecallBoard].pinned.isDefined), (true, true))
    )
  }

  coilixTest("the workshop runs its effects on the runtime of the test") {
    IO(assert(bean[IORuntime] eq munitIORuntime))
  }
}
