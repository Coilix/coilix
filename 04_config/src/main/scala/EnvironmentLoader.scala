package dev.coilix.config

/** Builds the [[dev.coilix.config.Environment]] when the application starts. */
trait EnvironmentLoader {

  /**
   * Loads the configuration, with `overrides` taking precedence over every other source.
   *
   * @param overrides
   *   values from the command line, keyed by configuration path
   * @throws dev.coilix.config.ConfigurationException
   *   if a configuration file cannot be read
   */
  def load(overrides: Map[String, String]): Environment
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
