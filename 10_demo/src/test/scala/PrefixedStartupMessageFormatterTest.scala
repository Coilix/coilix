package org.scaling.demo

import munit.FunSuite
import org.scaling.boot.DefaultStartupMessageFormatter
import scala.concurrent.duration.DurationInt

final class PrefixedStartupMessageFormatterTest extends FunSuite {
  private val formatter = PrefixedStartupMessageFormatter("[demo]", DefaultStartupMessageFormatter())

  test("prefixes the starting message") {
    assertEquals(formatter.starting("App"), "[demo] Starting App")
  }

  test("prefixes the started message") {
    assertEquals(formatter.started("App", 1.second), "[demo] Started App in 1.000 seconds")
  }
}
