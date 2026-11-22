package dev.coilix.boot

import munit.FunSuite

final class JvmMemoryOptionsTest extends FunSuite {
  private val gigabyte = 1024L * 1024L * 1024L

  test("warns when no option ends the JVM after an OutOfMemoryError") {
    val options = JvmMemoryOptions(Some(false), Some(false), 4 * gigabyte, Some(4 * gigabyte))
    assertEquals(
      options.warnings.map(_.startsWith("The JVM runs without -XX:+ExitOnOutOfMemoryError or -XX:+CrashOnOutOfMemoryError")),
      List(true)
    )
  }

  test("does not warn with -XX:+ExitOnOutOfMemoryError or -XX:+CrashOnOutOfMemoryError") {
    assertEquals(
      (
        JvmMemoryOptions(Some(true), Some(false), 4 * gigabyte, None).warnings,
        JvmMemoryOptions(Some(false), Some(true), 4 * gigabyte, None).warnings
      ),
      (Nil, Nil)
    )
  }

  test("does not warn when the JVM does not tell its options") {
    assertEquals(JvmMemoryOptions(None, None, gigabyte, None).warnings, Nil)
  }

  test("warns when the maximum heap is less than half of the available memory") {
    assertEquals(
      JvmMemoryOptions(Some(true), None, gigabyte, Some(4 * gigabyte)).warnings,
      List(
        "The maximum heap is 1024 MB, less than half of the 4096 MB available to the JVM. Set -XX:MaxRAMPercentage=75 or -Xmx to " +
          "use the memory of the container"
      )
    )
  }

  test("reads the options of the running JVM") {
    assert(JvmMemoryOptions.current.maxHeapBytes > 0)
  }
}
