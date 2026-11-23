package com.example
package catseffect

import dev.coilix.concurrency.TaskExecutor
import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class CatsEffectTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[CatsEffectApplication.type]

  test("IO runs on the runtime of the application") {
    setup.use {
      assertEquals(bean[ExchangeRates].ratesNow(List("EUR", "USD")).keySet, Set("EUR", "USD"))
    }
  }

  test("tasks of Coilix run on the compute pool of cats-effect") {
    setup.use {
      assert(Await.result(bean[TaskExecutor].submit(Thread.currentThread.getName), 5.seconds).startsWith("io-compute-"))
    }
  }
}
