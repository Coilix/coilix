package dev.coilix.boot

import cats.effect.IO
import dev.coilix.boot.application.ArgumentsAwareBean
import dev.coilix.boot.application.FixtureApplication
import dev.coilix.boot.application.RunJournal
import dev.coilix.boot.fixture.Clock
import dev.coilix.boot.scan.CustomScanApplication
import dev.coilix.boot.scan.NotAnnotatedApplication
import dev.coilix.config.ClasspathEnvironmentLoader
import dev.coilix.config.ConfigFormat
import dev.coilix.config.Environment
import dev.coilix.context.RegisteredBean
import munit.CatsEffectSuite
import scala.reflect.ClassTag

final class DefaultCoilixApplicationRunnerTest extends CatsEffectSuite {
  private val runner = DefaultCoilixApplicationRunner(
    SimpleArgumentsParser(),
    ClasspathEnvironmentLoader("runner", ConfigFormat.byPrecedence, getClass.getClassLoader),
    components => StartupComponents.default(using components)
  )

  private val application =
    ResourceSuiteLocalFixture("application", started[FixtureApplication.type](List("--name=Ann", "input.txt"), Nil))

  override def munitFixtures = List(application)

  private def context = application().context

  test("scans the package of the application class") {
    assert(context.getBeanOption[RunJournal].isDefined)
  }

  test("registers the arguments as a bean") {
    assertEquals(context.getBean[ApplicationArguments].nonOptionArgs, List("input.txt"))
  }

  test("injects the arguments into beans") {
    assertEquals(context.getBean[ArgumentsAwareBean].arguments.optionValue("name"), Some("Ann"))
  }

  test("registers the environment loaded from configuration files") {
    assertEquals(context.getBean[Environment].get[String]("fixture.mode"), Some("from-file"))
  }

  test("command line options override configuration files") {
    assertEquals(context.getBean[Environment].get[String]("name"), Some("Ann"))
  }

  test("returns an active context without running the runners") {
    assertEquals((context.isActive, context.getBean[RunJournal].recorded), (true, Nil))
  }

  test("scans the packages declared by the application") {
    started[CustomScanApplication.type](List("--name=Ann"), Nil).use(scanned =>
      IO(assertEquals(scanned.context.getBean[Clock].now, 42L))
    )
  }

  test("start builds the context with extra singletons") {
    val token = StartToken("till-1")
    started[FixtureApplication.type](List("--name=Bob"), List(RegisteredBean.of[StartToken]("token", token))).use(started =>
      IO(
        assertEquals(
          (
            started.context.getBean[StartToken] eq token,
            started.context.getBean[RunJournal].recorded,
            started.arguments.optionValue("name")
          ),
          (true, Nil, Some("Bob"))
        )
      )
    )
  }

  test("closes the context when the resource is released") {
    started[FixtureApplication.type](Nil, Nil).use(started => IO.pure(started.context)).map(context =>
      assertEquals(context.isActive, false)
    )
  }

  test("gives a startup error for a class without @CoilixApplication") {
    runner.start[NotAnnotatedApplication.type](Nil, Nil).value.use(result =>
      IO(assertEquals(result.left.toOption, Some(StartupError.NotABootApplication(classOf[NotAnnotatedApplication.type]))))
    )
  }

  private def started[T: ClassTag](args: List[String], singletons: List[RegisteredBean]) =
    StartupError.raised(runner.start[T](args, singletons))
}
