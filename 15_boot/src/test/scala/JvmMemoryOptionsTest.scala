package dev.coilix.boot

import java.nio.file.Files
import java.nio.file.Path
import munit.FunSuite

final class JvmMemoryOptionsTest extends FunSuite {
  private val gigabyte = 1024L * 1024L * 1024L

  private val emptyRoot = FunFixture[Path](_ => Files.createTempDirectory("root"), _ => ())

  test("warns when no option ends the JVM after an OutOfMemoryError") {
    val options = JvmMemoryOptions(Some(false), Some(false), 4 * gigabyte, Some(4 * gigabyte), inContainer = false)
    assertEquals(
      options.warnings.map(_.startsWith("The JVM runs without -XX:+ExitOnOutOfMemoryError or -XX:+CrashOnOutOfMemoryError")),
      List(true)
    )
  }

  test("does not warn with -XX:+ExitOnOutOfMemoryError or -XX:+CrashOnOutOfMemoryError") {
    assertEquals(
      (
        JvmMemoryOptions(Some(true), Some(false), 4 * gigabyte, None, inContainer = true).warnings,
        JvmMemoryOptions(Some(false), Some(true), 4 * gigabyte, None, inContainer = true).warnings
      ),
      (Nil, Nil)
    )
  }

  test("does not warn when the JVM does not tell its options") {
    assertEquals(JvmMemoryOptions(None, None, gigabyte, None, inContainer = true).warnings, Nil)
  }

  test("warns in a container when the maximum heap is less than half of the memory limit") {
    assertEquals(
      JvmMemoryOptions(Some(true), None, gigabyte, Some(4 * gigabyte), inContainer = true).warnings,
      List(
        "The maximum heap is 1024 MB, less than half of the 4096 MB limit of the container. Set -XX:MaxRAMPercentage=75 or -Xmx " +
          "to use the memory of the container"
      )
    )
  }

  test("does not check the heap outside a container") {
    assertEquals(JvmMemoryOptions(Some(true), None, gigabyte, Some(64 * gigabyte), inContainer = false).warnings, Nil)
  }

  emptyRoot.test("recognizes a container by its environment variables") { root =>
    assertEquals(
      (
        JvmMemoryOptions.runsInContainer(Map("KUBERNETES_SERVICE_HOST" -> "10.0.0.1"), root),
        JvmMemoryOptions.runsInContainer(Map("container" -> "podman"), root),
        JvmMemoryOptions.runsInContainer(Map("HOME" -> "/home/ann"), root)
      ),
      (true, true, false)
    )
  }

  emptyRoot.test("recognizes a container by the marker file of Docker") { root =>
    val _ = Files.createFile(root.resolve(".dockerenv"))
    assert(JvmMemoryOptions.runsInContainer(Map.empty, root))
  }

  emptyRoot.test("recognizes a container by the control group of the process") { root =>
    val proc = Files.createDirectories(root.resolve("proc/self"))
    val _    = Files.writeString(proc.resolve("cgroup"), "0::/kubepods/burstable/pod42/abc\n")
    assert(JvmMemoryOptions.runsInContainer(Map.empty, root))
  }

  test("reads the options of the running JVM") {
    assert(JvmMemoryOptions.current.maxHeapBytes > 0)
  }
}
