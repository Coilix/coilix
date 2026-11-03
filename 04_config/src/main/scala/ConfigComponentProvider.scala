package dev.coilix.config

import dev.coilix.annotation.Slf4s

@Slf4s
final class ConfigComponentProvider(override val environment: Environment) extends ComponentProvider {

  override def get[T: ConfigPath](default: => T): T =
    if (environment.contains(summon[ConfigPath[T]].path)) configured(summon[ConfigPath[T]])
    else default

  private def configured[T](configPath: ConfigPath[T]): T = {
    val component = configPath.componentType.cast(environment.bind(configPath.path, configPath.componentType))
    log.info(s"Using ${component.getClass.getName} configured at '${configPath.path}'")
    component
  }
}
