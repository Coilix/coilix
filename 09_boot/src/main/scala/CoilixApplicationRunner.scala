package dev.coilix.boot

import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.ConfigurableApplicationContext
import scala.reflect.ClassTag

trait CoilixApplicationRunner {
  def run[T: ClassTag](args: List[String]): ConfigurableApplicationContext
}

object CoilixApplicationRunner {
  def default: CoilixApplicationRunner =
    DefaultCoilixApplicationRunner(
      SimpleArgumentsParser(),
      EnvironmentLoader.default,
      components => StartupComponents.default(using components)
    )
}
