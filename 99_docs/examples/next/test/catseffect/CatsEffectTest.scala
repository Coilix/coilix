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

  test("beans receive the values of Resource and IO beans, and the resource closes with the application") {
    val (pool, report) = setup.use((bean[ConnectionPool], bean[MigrationReport]))
    assertEquals((report.applied, pool.isOpen), (3, false))
  }

  test("IO lifecycle methods fill the cache at startup and clear it when the application closes") {
    val (cache, size) = setup.use((bean[ProductCache], bean[ProductCache].size))
    assertEquals((size, cache.size), (2, 0))
  }

  test("an IO runner runs with the other runners") {
    assertEquals(setup.withRunners.use(bean[PriceCheck].checkedProducts), 2)
  }
}
