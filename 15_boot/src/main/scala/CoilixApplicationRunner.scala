package dev.coilix.boot

import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.RegisteredBean
import scala.reflect.ClassTag

/** Builds the context of an application, the synchronous part of the start of a [[dev.coilix.boot.CoilixIOApp]]. */
trait CoilixApplicationRunner {

  /**
   * Builds the context of application `T` with `singletons` as additional ready beans and calls no runner, so the caller, such
   * as [[dev.coilix.boot.CoilixIOStartup]], runs the runners and closes the context.
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
   * The runner used by [[dev.coilix.boot.CoilixIOStartup]]: a [[dev.coilix.boot.DefaultCoilixApplicationRunner]] with the
   * default arguments parser and configuration loader. The other startup components are taken from the loaded configuration.
   */
  def default: CoilixApplicationRunner =
    DefaultCoilixApplicationRunner(
      SimpleArgumentsParser(),
      EnvironmentLoader.default,
      components => StartupComponents.default(using components)
    )
}
