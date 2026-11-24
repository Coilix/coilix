package dev.coilix.pekko

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import java.util.Map as JMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

/**
 * Settings of the pekko module under `coilix.pekko`.
 *
 * ```yaml
 * coilix:
 *   pekko:
 *     system-name: shop
 *     shutdown-timeout: 20s
 *     config:
 *       pekko.stream.materializer.max-input-buffer-size: 32
 * ```
 *
 * @param systemName
 *   the name of the `ActorSystem`
 * @param shutdownTimeout
 *   how long the context waits for the coordinated shutdown of the `ActorSystem` when it closes
 * @param config
 *   the configuration of Pekko from `coilix.pekko.config`, with keys that start with `pekko`; there is no separate
 *   `application.conf`
 */
final case class PekkoSettings(
  systemName:      String         = "coilix",
  shutdownTimeout: FiniteDuration = 30.seconds,
  config:          Config         = ConfigFactory.empty()
)

/** Reading of [[dev.coilix.pekko.PekkoSettings]] from the configuration. */
object PekkoSettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.pekko"

  /** The settings under [[path]] in `environment`, with the defaults for the keys that are not set. */
  def from(environment: Environment): PekkoSettings = {
    val defaults      = PekkoSettings()
    PekkoSettings(
      systemName      = environment.get[String](s"$path.system-name").getOrElse(defaults.systemName),
      shutdownTimeout = environment.get[FiniteDuration](s"$path.shutdown-timeout").getOrElse(defaults.shutdownTimeout),
      config          = environment.get[AnyRef](s"$path.config").fold(defaults.config)(pekkoConfig)
    )
  }

  private def pekkoConfig(section: AnyRef): Config =
    section match {
      case values: JMap[?, ?] => ConfigFactory.parseMap(stringKeys(values))
      case _ => throw ConfigurationException(s"$path.config must be a section with the configuration of Pekko")
    }

  private def stringKeys(values: JMap[?, ?]): JMap[String, AnyRef] =
    values.asScala.map((key, value) => key.toString -> boxed(value)).asJava

  /** `value` as an object. The cast cannot fail, because the values of a Java map are objects. */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def boxed(value: Any): AnyRef = value.asInstanceOf[AnyRef]
}
