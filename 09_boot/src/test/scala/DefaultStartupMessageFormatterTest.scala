package io.github.coilix.boot

import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class DefaultStartupMessageFormatterTest extends FunSuite {
  private val formatter = DefaultStartupMessageFormatter()

  test("formats the starting message") {
    assertEquals(formatter.starting("DemoApplication"), "Starting DemoApplication")
  }

  test("formats the started message with seconds") {
    assertEquals(formatter.started("DemoApplication", 123.millis), "Started DemoApplication in 0.123 seconds")
  }

  test("formats whole seconds with three decimals") {
    assertEquals(formatter.started("DemoApplication", 2.seconds), "Started DemoApplication in 2.000 seconds")
  }
}
