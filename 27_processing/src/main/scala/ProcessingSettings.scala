package dev.coilix.processing

import dev.coilix.config.Environment
import java.util.Map as JavaMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

/**
 * The settings of background processors under `coilix.processors`.
 *
 * ```yaml
 * coilix.processors:
 *   autostart: true
 *   shutdown-timeout: 30s
 *   defaults:
 *     slots: 256
 *     restart: { max-attempts: 10 }
 *   invoice-export:
 *     shards: 4
 * ```
 *
 * @param autostart
 *   whether enabled processors start when the context has started
 * @param shutdownTimeout
 *   how long stopping a processor, and closing the application, waits for the shards
 * @param defaults
 *   the settings of every processor, under `defaults`
 * @param processors
 *   the settings of each processor by its name
 */
final case class ProcessingSettings(
  autostart:       Boolean                        = true,
  shutdownTimeout: FiniteDuration                 = 30.seconds,
  defaults:        ProcessorSettings              = ProcessorSettings(),
  processors:      Map[String, ProcessorSettings] = Map.empty
) {

  /** The settings of the processor, with missing keys taken from `defaults`. */
  def of(id: ProcessorId): ProcessorSettings =
    processors.getOrElse(id.name, ProcessorSettings()).orElse(defaults)

  /** The configured names that belong to none of the given processors, usually misspelt. */
  def unknown(ids: List[ProcessorId]): List[String] =
    processors.keySet.diff(ids.map(_.name).toSet).toList.sorted
}

/** Reads the settings of background processors. */
object ProcessingSettings {

  /** The section of the settings. */
  val path: String = "coilix.processors"

  private val reserved = Set("autostart", "shutdown-timeout", "defaults")

  /** Reads `coilix.processors`, with the defaults of Coilix for missing keys. */
  def from(environment: Environment): ProcessingSettings = {
    val defaults       = ProcessingSettings()
    ProcessingSettings(
      autostart       = environment.get[Boolean](s"$path.autostart").getOrElse(defaults.autostart),
      shutdownTimeout = environment.get[FiniteDuration](s"$path.shutdown-timeout").getOrElse(defaults.shutdownTimeout),
      defaults        = ProcessorSettings.read(environment, s"$path.defaults", withShards = false),
      processors      =
        names(environment).map(name => name -> ProcessorSettings.read(environment, s"$path.$name", withShards = true)).toMap
    )
  }

  private def names(environment: Environment): List[String] =
    environment.get[AnyRef](path) match {
      case Some(section: JavaMap[?, ?]) => section.keySet.asScala.map(_.toString).filterNot(reserved).toList
      case _                            => Nil
    }
}
