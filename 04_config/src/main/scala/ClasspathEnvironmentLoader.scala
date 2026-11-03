package dev.coilix.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import dev.coilix.annotation.Slf4s
import java.net.URL
import scala.jdk.CollectionConverters.*

@Slf4s
final class ClasspathEnvironmentLoader(baseName: String, formats: List[ConfigFormat], classLoader: ClassLoader)
    extends EnvironmentLoader {

  override def load(overrides: Map[String, String]): Environment =
    Environment.of(TypesafeConfigTree(layers(overrides).reduceLeft(_.withFallback(_)).resolve()))

  private def layers(overrides: Map[String, String]): List[Config] =
    ConfigFactory.parseMap(overrides.asJava, "command line") :: systemProperties :: files

  private def systemProperties: Config =
    ConfigFactory.parseProperties(System.getProperties, ConfigParseOptions.defaults().setOriginDescription("system properties"))

  private def files: List[Config] =
    formats.flatMap(format => format.extensions.flatMap(extension => fileLayer(format, s"$baseName.$extension")))

  private def fileLayer(format: ConfigFormat, resource: String): Option[Config] =
    Option(classLoader.getResource(resource)).map(url => parsed(format, resource, url))

  private def parsed(format: ConfigFormat, resource: String, url: URL): Config = {
    log.info(s"Loading configuration from $resource")
    format.parse(url)
  }
}
