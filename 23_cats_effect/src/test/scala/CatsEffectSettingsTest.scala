package dev.coilix.catseffect

import com.typesafe.config.ConfigFactory
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class CatsEffectSettingsTest extends FunSuite {

  test("waits 30 seconds for each finalizer by default") {
    assertEquals(CatsEffectSettings.from(Environment.empty), CatsEffectSettings(shutdownTimeout = 30.seconds))
  }

  test("reads the limit from coilix.cats-effect.shutdown-timeout") {
    assertEquals(
      CatsEffectSettings.from(
        Environment.of(TypesafeConfigTree(ConfigFactory.parseString("coilix.cats-effect.shutdown-timeout = 1m")))
      ).shutdownTimeout,
      1.minute
    )
  }
}
