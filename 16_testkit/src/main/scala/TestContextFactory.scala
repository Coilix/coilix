package dev.coilix.testkit

import dev.coilix.boot.SimpleArgumentsParser
import dev.coilix.boot.StartupComponents
import dev.coilix.config.EnvironmentLoader
import dev.coilix.context.ConfigurableApplicationContext

/** Builds the application context of a test from a [[dev.coilix.testkit.CoilixTestSetup]]. */
trait TestContextFactory {

  /**
   * Builds and returns an open context. The caller closes it, which [[dev.coilix.testkit.CoilixFixtures]] does after the test.
   *
   * @throws dev.coilix.boot.UnmatchedBeanReplacementException
   *   if a replaced bean type matches no bean of the application
   * @throws dev.coilix.common.CoilixException
   *   if the configuration or the beans are invalid, for example when a dependency is missing
   */
  def create(setup: CoilixTestSetup): ConfigurableApplicationContext
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
