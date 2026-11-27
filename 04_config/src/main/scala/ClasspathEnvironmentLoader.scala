package dev.coilix.config

import cats.syntax.all.*
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
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
 *  1. files named `<baseName>-<profile>.<extension>` for each profile listed under `coilix.profiles.active`, where a profile
 *     listed later wins over one listed earlier,
 *  1. files named `<baseName>.<extension>` on the classpath, in the order of `formats`.
 *
 * The active profiles are read from the command line, the system properties and the base files, so a profile file cannot
 * activate further profiles.
 *
 * Missing files are skipped. Each file that is found is logged at `INFO` level. HOCON substitutions such as `${other.key}` are
 * resolved after the sources are merged, so they can refer to keys from any source. A file that cannot be parsed, a
 * substitution that cannot be resolved and a malformed list of profiles are returned as a [[dev.coilix.config.ConfigError]].
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
case class ClasspathEnvironmentLoader(baseName: String, formats: List[ConfigFormat], classLoader: ClassLoader)
    extends EnvironmentLoader {

  private type Loaded[A] = Either[ConfigError, A]

  override def load(overrides: Map[String, String]): Loaded[Environment] =
    for {
      baseFiles <- files(baseName)
      base       = ConfigFactory.parseMap(overrides.asJava, "command line") :: systemProperties :: baseFiles
      profiles  <- merged(base).map(TypesafeConfigTree(_)).flatMap(Profiles.active)
      _          = if (profiles.nonEmpty) log.info(s"Active profiles: ${profiles.mkString(", ")}")
      layers    <- profiles.reverse.flatTraverse(profileFiles)
      config    <- merged(base.take(2) ++ layers ++ base.drop(2))
    } yield Environment.of(TypesafeConfigTree(config))

  private def merged(layers: List[Config]): Loaded[Config] =
    try {
      Right(layers.reduceLeft(_.withFallback(_)).resolve())
    } catch {
      case error: ConfigException => Left(ConfigError.Unresolved(error.getMessage))
    }

  private def profileFiles(profile: String): Loaded[List[Config]] =
    files(s"$baseName-$profile")

  private def systemProperties: Config =
    ConfigFactory.parseProperties(System.getProperties, ConfigParseOptions.defaults().setOriginDescription("system properties"))

  private def files(name: String): Loaded[List[Config]] =
    formats.flatTraverse(format => format.extensions.flatTraverse(extension => fileLayer(format, s"$name.$extension")))

  private def fileLayer(format: ConfigFormat, resource: String): Loaded[List[Config]] =
    Option(classLoader.getResource(resource)).toList.traverse(url => parsed(format, resource, url))

  private def parsed(format: ConfigFormat, resource: String, url: URL): Loaded[Config] = {
    log.info(s"Loading configuration from $resource")
    format.parse(url)
  }
}
