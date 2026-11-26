package dev.coilix.boot

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.RegisteredBean
import scala.reflect.ClassTag

/** Builds the context of an application, the first part of the start of a [[dev.coilix.boot.CoilixIOApp]]. */
trait CoilixApplicationRunner {

  /**
   * The context of application `T` with `singletons` as additional ready beans, as a `Resource` released by
   * [[dev.coilix.context.ConfigurableApplicationContext.shutdown]]. It calls no runner and does not start the context, so the
   * caller, such as [[dev.coilix.boot.CoilixIOStartup]], starts it and runs the runners.
   *
   * @param singletons
   *   ready instances registered before the beans are created, such as a runtime that the caller owns
   * @return
   *   the context with the parsed arguments
   */
  def start[T: ClassTag](args: List[String], singletons: List[RegisteredBean]): Resource[IO, StartedApplication]
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
