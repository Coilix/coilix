package dev.coilix.boot

import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.RegisteredBean
import scala.reflect.ClassTag

/** Runs the startup sequence behind [[dev.coilix.boot.Coilix.run]]. */
trait CoilixApplicationRunner {

  /** Starts application `T` with `args`, as described in [[dev.coilix.boot.Coilix.run]]. */
  def run[T: ClassTag](args: List[String]): ConfigurableApplicationContext

  /**
   * Builds the context of application `T` like [[run]], with `singletons` as additional ready beans, but registers no shutdown
   * hook and calls no runner, for a start that runs them itself, such as `CoilixIOApp` from `coilix-cats-effect`.
   *
   * @param singletons
   *   ready instances registered before the beans are created, such as a runtime that the caller owns
   * @return
   *   the context, whose runners the caller runs and which the caller closes
   */
  def start[T: ClassTag](args: List[String], singletons: List[RegisteredBean]): StartedApplication
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
