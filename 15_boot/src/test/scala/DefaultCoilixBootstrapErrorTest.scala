package dev.coilix.boot

import cats.effect.IO
import dev.coilix.boot.collected.NightlyJob
import dev.coilix.boot.collectedconflict.DoubleJob
import dev.coilix.boot.fixture.Clock
import dev.coilix.boot.jobs.JobsModule
import dev.coilix.boot.modules.EffectModule
import dev.coilix.boot.modules.FixedModuleLoader
import dev.coilix.common.GenericType
import dev.coilix.config.ComponentProvider
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.ClassPathScanner
import dev.coilix.context.CoilixModule
import dev.coilix.context.ComponentCollector
import dev.coilix.context.ContextError
import munit.CatsEffectSuite

final class DefaultCoilixBootstrapErrorTest extends CatsEffectSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("gives an error when a replacement matches no discovered bean") {
    errorOf(Nil, List("dev.coilix.boot.empty"), BeanReplacements.of[Clock])
      .map(error => assertEquals(error, Some(StartupError.UnmatchedBeanReplacement(List(GenericType.of(classOf[Clock]))))))
  }

  test("gives an error when a collected class also has a component annotation") {
    errorOf(List(JobsModule()), List("dev.coilix.boot.collectedconflict"))
      .map(error => assertEquals(error, Some(StartupError.CollectedComponentConflict(classOf[DoubleJob], "JavaBean"))))
  }

  test("gives an error when a bean requires a collected class") {
    errorOf(List(JobsModule()), List("dev.coilix.boot.collected", "dev.coilix.boot.collectedinjection")).map(error =>
      assertEquals(error, Some(StartupError.CollectedComponentRequired("nightlyJobUser", classOf[NightlyJob], "JavaBean")))
    )
  }

  test("gives an error when two modules give an effect interpreter") {
    errorOf(List(EffectModule("io"), EffectModule("zio")), List("dev.coilix.boot.fixture"))
      .map(error => assertEquals(error, Some(StartupError.ConflictingEffectInterpreters(List("io", "zio")))))
  }

  test("keeps the typed error of the container") {
    errorOf(Nil, List("dev.coilix.boot.collectedinjection")).map(error =>
      assertEquals(
        error.collect {
          case StartupError.Context(ContextError.MissingBean(description)) => description.contains("nightlyJobUser")
        },
        Some(true)
      )
    )
  }

  private def errorOf(
    modules:      List[CoilixModule],
    basePackages: List[String],
    replacements: BeanReplacements = BeanReplacements.none
  ): IO[Option[StartupError]] =
    bootstrapWith(modules).start(basePackages, Nil, Nil, replacements).value.use(result => IO.pure(result.left.toOption))

  private def bootstrapWith(modules: List[CoilixModule]): CoilixBootstrap =
    DefaultCoilixBootstrap(
      ClassPathScanner.default,
      ApplicationContextBuilder.default,
      FixedModuleLoader(modules),
      BeanDefinitionFactory.default,
      ComponentCollector.default
    )
}
