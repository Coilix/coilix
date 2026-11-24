package dev.coilix.testkit.catseffect

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.catseffect.repo.Repo
import munit.CatsEffectSuite

final class CoilixIOSuiteTest extends CatsEffectSuite with CoilixIOSuite {
  private given CoilixTestSetup = CoilixTestSetup.forPackages("dev.coilix.testkit.catseffect.repo")

  coilixTest("runs an IO body with the beans of the context") {
    bean[Repo[IO]].find(7).map(assertEquals(_, None))
  }

  coilixTest("uses the runtime of the suite as the ioRuntime bean") {
    IO(assertEquals((bean[IORuntime] eq munitIORuntime, context.beanNames.contains("coilixIORuntime")), (true, false)))
  }

  coilixTest("takes an explicit setup", summon[CoilixTestSetup].withProperties("shop.name" -> "Corner")) {
    IO(assertEquals(property[String]("shop.name"), Some("Corner")))
  }
}
