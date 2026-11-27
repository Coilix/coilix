package dev.coilix.config

import dev.coilix.annotation.Slf4s

/**
 * Creates configured components by binding the section at their [[dev.coilix.config.ConfigPath]] with
 * [[dev.coilix.config.Environment.binding]].
 *
 * Each configured component is logged at `INFO` level with its class and path. This is the implementation returned by
 * [[dev.coilix.config.ComponentProvider.from]].
 */
@Slf4s
case class ConfigComponentProvider(override val environment: Environment) extends ComponentProvider {

  override def lookup[T: ConfigPath](default: => T): Either[ConfigError, T] =
    if (environment.contains(summon[ConfigPath[T]].path)) configured(summon[ConfigPath[T]])
    else Right(default)

  private def configured[T](configPath: ConfigPath[T]): Either[ConfigError, T] =
    environment.binding(configPath.path, configPath.componentType).map(configPath.componentType.cast).map(logged(configPath))

  private def logged[T](configPath: ConfigPath[T])(component: T): T = {
    log.info(s"Using ${component.getClass.getName} configured at '${configPath.path}'")
    component
  }
}
