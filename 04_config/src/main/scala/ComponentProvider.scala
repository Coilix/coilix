package dev.coilix.config

/**
 * Chooses the implementation of a replaceable framework component.
 *
 * Every replaceable component declares a [[dev.coilix.config.ConfigPath]] in its companion object and gets its
 * implementation from the provider. When the configuration has a section at that path, the provider creates the component
 * from it, using the `class` key to pick the implementation. Otherwise it returns the built-in default.
 *
 * ```scala
 * object StartupMessageFormatter {
 *   given ConfigPath[StartupMessageFormatter] = ConfigPath[StartupMessageFormatter]("coilix.boot.startup-message-formatter")
 *
 *   def default(using components: ComponentProvider): StartupMessageFormatter =
 *     components.get[StartupMessageFormatter](DefaultStartupMessageFormatter())
 * }
 * ```
 *
 * An application replaces the component in its configuration. The other keys of the section are passed to the constructor
 * of the configured class:
 *
 * ```yaml
 * coilix:
 *   boot:
 *     startup-message-formatter:
 *       class: com.example.shop.ShopStartupMessageFormatter
 *       prefix: "[shop]"
 * ```
 */
trait ComponentProvider {

  /** The environment from which configured components are created. */
  def environment: Environment

  /**
   * Returns the component configured at the [[dev.coilix.config.ConfigPath]] of `T`, or `default` when that path is not set.
   *
   * `default` is evaluated only when it is used.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if the path is set but the component cannot be created from it
   */
  def get[T: ConfigPath](default: => T): T
}

/** Factories for [[dev.coilix.config.ComponentProvider]]. */
object ComponentProvider {

  /** A provider that creates configured components from `environment`. */
  def from(environment: Environment): ComponentProvider = ConfigComponentProvider(environment)

  /** A provider with an empty environment, which always returns the built-in default. */
  def builtIn: ComponentProvider = from(Environment.empty)
}
