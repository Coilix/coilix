package dev.coilix.boot

import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class DefaultStartupMessageFormatterTest extends FunSuite {
  private val formatter = DefaultStartupMessageFormatter()

  test("formats the starting message") {
    assertEquals(formatter.starting("DemoApplication"), "Launching DemoApplication")
  }

  test("formats the started message with seconds") {
    assertEquals(formatter.started("DemoApplication", 123.millis), "DemoApplication is up after 0.123 s")
  }

  test("formats whole seconds with three decimals") {
    assertEquals(formatter.started("DemoApplication", 2.seconds), "DemoApplication is up after 2.000 s")
  }
}
