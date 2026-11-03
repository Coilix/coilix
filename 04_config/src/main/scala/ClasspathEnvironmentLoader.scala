package dev.coilix.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import dev.coilix.annotation.Slf4s
import java.net.URL
import scala.jdk.CollectionConverters.*

/**
 * Loads the environment from the command line, JVM system properties and configuration files on the classpath.
 *
 * Sources are merged so that the first one that defines a key wins:
 *
 *  1. the `overrides` passed to [[dev.coilix.config.ClasspathEnvironmentLoader.load]], which Coilix fills from command-line
 *     options such as `--server.port=9090`,
 *  1. JVM system properties, such as `-Dserver.port=9090`,
 *  1. files named `<baseName>.<extension>` on the classpath, in the order of `formats`.
 *
 * Missing files are skipped. Each file that is found is logged at `INFO` level. HOCON substitutions such as `${other.key}` are
 * resolved after the sources are merged, so they can refer to keys from any source.
 *
 * This is the implementation returned by [[dev.coilix.config.EnvironmentLoader.default]].
 *
 * @param baseName
 *   file name without extension, `application` by default
 * @param formats
 *   supported file formats, from the highest to the lowest precedence
 * @param classLoader
 *   class loader used to find the files
 */
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
