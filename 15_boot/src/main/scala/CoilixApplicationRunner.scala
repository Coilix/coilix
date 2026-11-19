package dev.coilix.boot

import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.ConfigurableApplicationContext
import scala.reflect.ClassTag

/** Runs the startup sequence behind [[dev.coilix.boot.Coilix.run]]. */
trait CoilixApplicationRunner {

  /** Starts application `T` with `args`, as described in [[dev.coilix.boot.Coilix.run]]. */
  def run[T: ClassTag](args: List[String]): ConfigurableApplicationContext
}

/** Factories for [[dev.coilix.boot.CoilixApplicationRunner]]. */
object CoilixApplicationRunner {

  /**
   * The runner used by [[dev.coilix.boot.Coilix.run]]: a [[dev.coilix.boot.DefaultCoilixApplicationRunner]] with the default
   * arguments parser and
   * configuration loader. The other startup components are taken from the loaded configuration.
   */
  def default: CoilixApplicationRunner =
    DefaultCoilixApplicationRunner(
      SimpleArgumentsParser(),
      EnvironmentLoader.default,
      components => StartupComponents.default(using components)
    )
}
