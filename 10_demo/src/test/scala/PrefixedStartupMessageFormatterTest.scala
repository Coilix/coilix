package io.github.coilix.demo

import io.github.coilix.boot.DefaultStartupMessageFormatter
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class PrefixedStartupMessageFormatterTest extends FunSuite {
  private val formatter = PrefixedStartupMessageFormatter("[demo]", DefaultStartupMessageFormatter())

  test("prefixes the starting message") {
    assertEquals(formatter.starting("App"), "[demo] Launching App")
  }

  test("prefixes the started message") {
    assertEquals(formatter.started("App", 1.second), "[demo] App is up after 1.000 s")
  }
}
