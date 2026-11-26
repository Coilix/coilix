package dev.coilix.boot

import dev.coilix.boot.application.ArgumentsAwareBean
import dev.coilix.boot.application.FixtureApplication
import dev.coilix.boot.application.RunJournal
import dev.coilix.boot.fixture.Clock
import dev.coilix.boot.scan.CustomScanApplication
import dev.coilix.config.ClasspathEnvironmentLoader
import dev.coilix.config.ConfigFormat
import dev.coilix.config.Environment
import dev.coilix.context.RegisteredBean
import munit.FunSuite

final class DefaultCoilixApplicationRunnerTest extends FunSuite {
  private val runner = DefaultCoilixApplicationRunner(
    SimpleArgumentsParser(),
    ClasspathEnvironmentLoader("runner", ConfigFormat.byPrecedence, getClass.getClassLoader),
    components => StartupComponents.default(using components)
  )

  private val application = runner.start[FixtureApplication.type](List("--name=Ann", "input.txt"), Nil)

  private val context = application.context

  override def afterAll(): Unit = context.close()

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
    val scanned = runner.start[CustomScanApplication.type](List("--name=Ann"), Nil)
    try {
      assertEquals(scanned.context.getBean[Clock].now, 42L)
    } finally {
      scanned.context.close()
    }
  }

  test("start builds the context with extra singletons") {
    val token   = StartToken("till-1")
    val started = runner.start[FixtureApplication.type](List("--name=Bob"), List(RegisteredBean.of[StartToken]("token", token)))
    try {
      assertEquals(
        (
          started.context.getBean[StartToken] eq token,
          started.context.getBean[RunJournal].recorded,
          started.arguments.optionValue("name")
        ),
        (true, Nil, Some("Bob"))
      )
    } finally {
      started.context.close()
    }
  }
}
