package dev.coilix.boot

import com.sun.management.HotSpotDiagnosticMXBean
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
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
 */
final case class JvmMemoryOptions(
  exitOnOutOfMemory:    Option[Boolean],
  crashOnOutOfMemory:   Option[Boolean],
  maxHeapBytes:         Long,
  availableMemoryBytes: Option[Long]
) {

  /**
   * Warnings about options that make the application fragile: no option that ends the JVM after an `OutOfMemoryError`, and a
   * maximum heap smaller than half of the available memory, such as the default 25% of a container.
   */
  def warnings: List[String] =
    List(exitWarning, heapWarning).flatten

  private def exitWarning: Option[String] =
    Option.when(exitOnOutOfMemory.contains(false) && crashOnOutOfMemory.contains(false))(
      "The JVM runs without -XX:+ExitOnOutOfMemoryError or -XX:+CrashOnOutOfMemoryError, so an OutOfMemoryError in a thread " +
        "that Coilix does not manage can leave the application running half-broken"
    )

  private def heapWarning: Option[String] =
    availableMemoryBytes.filter(available => maxHeapBytes < available / 2).map { available =>
      s"The maximum heap is ${megabytes(maxHeapBytes)} MB, less than half of the ${megabytes(available)} MB available to the " +
        "JVM. Set -XX:MaxRAMPercentage=75 or -Xmx to use the memory of the container"
    }

  private def megabytes(bytes: Long): Long = bytes / (1024L * 1024L)
}

/** Reading of [[dev.coilix.boot.JvmMemoryOptions]] from the running JVM. */
object JvmMemoryOptions {

  /** The options of the running JVM. */
  def current: JvmMemoryOptions =
    JvmMemoryOptions(
      booleanOption("ExitOnOutOfMemoryError"),
      booleanOption("CrashOnOutOfMemoryError"),
      Runtime.getRuntime.maxMemory,
      Try(ManagementFactory.getPlatformMXBean(classOf[OperatingSystemMXBean]).getTotalMemorySize).toOption
    )

  private def booleanOption(name: String): Option[Boolean] =
    Try(ManagementFactory.getPlatformMXBean(classOf[HotSpotDiagnosticMXBean]).getVMOption(name).getValue.toBoolean).toOption
}
