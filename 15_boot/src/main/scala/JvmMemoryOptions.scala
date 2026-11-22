package dev.coilix.boot

import com.sun.management.HotSpotDiagnosticMXBean
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.util.Try

/**
 * The options of the JVM that decide how the application behaves when memory runs out, checked when `Coilix.run` starts.
 *
 * @param exitOnOutOfMemory
 *   whether `-XX:+ExitOnOutOfMemoryError` is set, or `None` when the JVM does not tell
 * @param crashOnOutOfMemory
 *   whether `-XX:+CrashOnOutOfMemoryError` is set, or `None` when the JVM does not tell
 * @param maxHeapBytes
 *   the largest heap the JVM may use
 * @param availableMemoryBytes
 *   the memory available to the JVM, which is the limit of the container in a container, or `None` when the JVM does not tell
 * @param inContainer
 *   whether the JVM runs in a container, where the memory limit is meant for this one application
 */
final case class JvmMemoryOptions(
  exitOnOutOfMemory:    Option[Boolean],
  crashOnOutOfMemory:   Option[Boolean],
  maxHeapBytes:         Long,
  availableMemoryBytes: Option[Long],
  inContainer:          Boolean
) {

  /**
   * Warnings about options that make the application fragile: no option that ends the JVM after an `OutOfMemoryError`, and,
   * in a container, a maximum heap smaller than half of the memory limit, such as the default 25%. Outside a container the
   * memory is shared with other programs, so the heap is not checked.
   */
  def warnings: List[String] =
    List(exitWarning, heapWarning).flatten

  private def exitWarning: Option[String] =
    Option.when(exitOnOutOfMemory.contains(false) && crashOnOutOfMemory.contains(false))(
      "The JVM runs without -XX:+ExitOnOutOfMemoryError or -XX:+CrashOnOutOfMemoryError, so an OutOfMemoryError in a thread " +
        "that Coilix does not manage can leave the application running half-broken"
    )

  private def heapWarning: Option[String] =
    availableMemoryBytes.filter(available => inContainer && maxHeapBytes < available / 2).map { available =>
      s"The maximum heap is ${megabytes(maxHeapBytes)} MB, less than half of the ${megabytes(available)} MB limit of the " +
        "container. Set -XX:MaxRAMPercentage=75 or -Xmx to use the memory of the container"
    }

  private def megabytes(bytes: Long): Long = bytes / (1024L * 1024L)
}

/** Reading of [[dev.coilix.boot.JvmMemoryOptions]] from the running JVM. */
object JvmMemoryOptions {

  private val containerMarkers = List("/.dockerenv", "/run/.containerenv")

  private val containerCgroups = List("kubepods", "docker", "containerd", "libpod", "lxc")

  /** The options of the running JVM. */
  def current: JvmMemoryOptions =
    JvmMemoryOptions(
      booleanOption("ExitOnOutOfMemoryError"),
      booleanOption("CrashOnOutOfMemoryError"),
      Runtime.getRuntime.maxMemory,
      Try(ManagementFactory.getPlatformMXBean(classOf[OperatingSystemMXBean]).getTotalMemorySize).toOption,
      runsInContainer(sys.env, Paths.get("/"))
    )

  /**
   * Whether the process runs in a container: Kubernetes sets `KUBERNETES_SERVICE_HOST`, Podman sets `container`, Docker and
   * Podman leave a marker file in the root, and the control group of the process names the container runtime.
   *
   * @param root
   *   the root of the file system, replaced in tests
   */
  def runsInContainer(environment: Map[String, String], root: Path): Boolean =
    environment.contains("KUBERNETES_SERVICE_HOST") ||
      environment.contains("container") ||
      containerMarkers.exists(marker => Files.exists(root.resolve(marker.drop(1)))) ||
      cgroupNamesRuntime(root.resolve("proc/self/cgroup"))

  private def cgroupNamesRuntime(cgroup: Path): Boolean =
    Try(Files.readString(cgroup)).toOption.exists(content => containerCgroups.exists(content.contains))

  private def booleanOption(name: String): Option[Boolean] =
    Try(ManagementFactory.getPlatformMXBean(classOf[HotSpotDiagnosticMXBean]).getVMOption(name).getValue.toBoolean).toOption
}
