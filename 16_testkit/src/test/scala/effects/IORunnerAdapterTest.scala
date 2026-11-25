package dev.coilix.testkit
package effects

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.IOApplicationRunner
import dev.coilix.boot.IORunnerAdapter
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import dev.coilix.testkit.effects.warmup.AuditRunner
import dev.coilix.testkit.effects.warmup.WarmupJournal
import munit.FunSuite

final class IORunnerAdapterTest extends FunSuite with CoilixContextOps {

  private val setup =
    CoilixTestSetup.forPackages("dev.coilix.testkit.effects.warmup").withProperties("coilix.concurrency.compute.threads" -> "2")

  test("finds the IO runners of the context") {
    setup.use {
      val adapter = IORunnerAdapter(bean[IORuntime])
      assertEquals(adapter.runners(summon).map(_.getClass.getSimpleName).toSet, Set("PriceRunner", "AuditRunner"))
    }
  }

  test("waits for the effect of a runner") {
    setup.use {
      IORunnerAdapter(bean[IORuntime]).run(bean[AuditRunner], ApplicationArguments.empty)
      assertEquals(bean[WarmupJournal].all.last, "audit")
    }
  }

  test("rethrows the failure of a runner") {
    setup.use {
      val failing: IOApplicationRunner = _ => IO.raiseError(IllegalStateException("no prices"))
      val failure =
        intercept[IllegalStateException](IORunnerAdapter(bean[IORuntime]).run(failing, ApplicationArguments.empty))
      assertEquals(failure.getMessage, "no prices")
    }
  }

  test("ignores a bean that is not an IO runner") {
    setup.use {
      IORunnerAdapter(bean[IORuntime]).run("not a runner", ApplicationArguments.empty)
    }
  }
}
