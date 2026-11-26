package dev.coilix.testkit
package effects

import cats.effect.IO
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.IOApplicationRunner
import dev.coilix.boot.IORunnerAdapter
import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.effects.warmup.AuditRunner
import dev.coilix.testkit.effects.warmup.WarmupJournal
import munit.CatsEffectSuite

final class IORunnerAdapterTest extends CatsEffectSuite with CoilixIOSuite {

  private given CoilixTestSetup =
    CoilixTestSetup.forPackages("dev.coilix.testkit.effects.warmup").withProperties("coilix.concurrency.compute.threads" -> "2")

  coilixTest("finds the IO runners of the context") {
    IO(assertEquals(IORunnerAdapter().runners(summon).map(_.getClass.getSimpleName).toSet, Set("PriceRunner", "AuditRunner")))
  }

  coilixTest("gives the effect of a runner") {
    IORunnerAdapter().run(bean[AuditRunner], ApplicationArguments.empty).map(_ =>
      assertEquals(bean[WarmupJournal].all.last, "audit")
    )
  }

  coilixTest("fails with the failure of a runner") {
    val failing: IOApplicationRunner = _ => IO.raiseError(IllegalStateException("no prices"))
    IORunnerAdapter().run(failing, ApplicationArguments.empty).attempt.map(result =>
      assertEquals(result.left.map(_.getMessage), Left("no prices"))
    )
  }

  coilixTest("ignores a bean that is not an IO runner") {
    IORunnerAdapter().run("not a runner", ApplicationArguments.empty)
  }
}
