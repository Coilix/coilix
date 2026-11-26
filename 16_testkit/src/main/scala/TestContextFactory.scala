package dev.coilix.testkit

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.boot.SimpleArgumentsParser
import dev.coilix.boot.StartupComponents
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.context.SynchronousLifecycle

/** Builds the application context of a test from a [[dev.coilix.testkit.CoilixTestSetup]]. */
trait TestContextFactory {

  /**
   * The started context of `setup` as a `Resource`, released by [[dev.coilix.context.ConfigurableApplicationContext.shutdown]].
   *
   * @return
   *   a resource that fails with an [[dev.coilix.boot.UnmatchedBeanReplacementException]] when a replaced bean type matches no
   *   bean of the application, and with a [[dev.coilix.common.CoilixException]] when the configuration or the beans are invalid,
   *   for example when a dependency is missing
   */
  def resource(setup: CoilixTestSetup): Resource[IO, ConfigurableApplicationContext]

  /**
   * Opens [[dev.coilix.testkit.TestContextFactory.resource]] with [[dev.coilix.context.SynchronousLifecycle.opened]], for a
   * synchronous suite. The caller closes the context, which [[dev.coilix.testkit.CoilixFixtures]] does after the test.
   *
   * @throws dev.coilix.boot.UnmatchedBeanReplacementException
   *   if a replaced bean type matches no bean of the application
   * @throws dev.coilix.common.CoilixException
   *   if the configuration or the beans are invalid, for example when a dependency is missing
   */
  final def create(setup: CoilixTestSetup): ConfigurableApplicationContext =
    SynchronousLifecycle.opened(resource(setup))
}

/** Factories for [[dev.coilix.testkit.TestContextFactory]]. */
object TestContextFactory {

  /**
   * A [[dev.coilix.testkit.DefaultTestContextFactory]] with the default arguments parser and configuration loader. The other
   * startup components, such as the bootstrap, are taken from the loaded configuration, so a replacement under their `coilix.*`
   * path applies to tests too.
   */
  def default: TestContextFactory =
    DefaultTestContextFactory(
      SimpleArgumentsParser(),
      EnvironmentLoader.default,
      components => StartupComponents.default(using components)
    )
}
