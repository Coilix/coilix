package dev.coilix.config

/** Builds the [[dev.coilix.config.Environment]] when the application starts. */
trait EnvironmentLoader {

  /**
   * Loads the configuration, with `overrides` taking precedence over every other source.
   *
   * @param overrides
   *   values from the command line, keyed by configuration path
   * @return
   *   the environment, or the [[dev.coilix.config.ConfigError]] if a configuration source cannot be parsed or resolved
   */
  def load(overrides: Map[String, String]): Either[ConfigError, Environment]
}

/** Factories for [[dev.coilix.config.EnvironmentLoader]]. */
object EnvironmentLoader {

  /**
   * The loader used by Coilix: a [[dev.coilix.config.ClasspathEnvironmentLoader]] that reads `application.properties`,
   * `application.yaml`, `application.yml` and `application.conf` from the context class loader of the current thread.
   */
  def default: EnvironmentLoader =
    ClasspathEnvironmentLoader("application", ConfigFormat.byPrecedence, Thread.currentThread().getContextClassLoader)
}
