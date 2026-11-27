package dev.coilix.concurrency

import com.typesafe.config.ConfigFactory
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ConcurrencySettingsTest extends FunSuite {

  test("uses the defaults when nothing is configured") {
    assertEquals(ConcurrencySettings.from(Environment.empty), ConcurrencySettings())
  }

  test("reads the configured keys and keeps the defaults of the others") {
    val settings = ConcurrencySettings.from(environment("""
      coilix.concurrency.compute.threads = 3
      coilix.concurrency.blocking { virtual-threads = false, max-concurrency = 10 }
    """))
    assertEquals(
      settings,
      ConcurrencySettings(
        ComputeSettings(threads         = 3),
        BlockingSettings(virtualThreads = false, maxConcurrency = Some(10))
      )
    )
  }

  private def environment(hocon: String): Environment =
    Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon)))
}
