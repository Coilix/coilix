package dev.coilix.boot

import dev.coilix.boot.application.ArgumentsAwareBean
import dev.coilix.boot.application.FixtureApplication
import dev.coilix.boot.application.RunJournal
import dev.coilix.config.ClasspathEnvironmentLoader
import dev.coilix.config.ConfigFormat
import dev.coilix.config.Environment
import dev.coilix.context.ConfigurableApplicationContext
import java.util.concurrent.atomic.AtomicReference
import munit.FunSuite

final class DefaultCoilixApplicationRunnerTest extends FunSuite {
  private val hooked = AtomicReference(List.empty[ConfigurableApplicationContext])

  private val registrar = new ShutdownHookRegistrar {
    override def register(context: ConfigurableApplicationContext): Thread = {
      val _ = hooked.updateAndGet(_ :+ context)
      Thread(() => context.close())
    }
  }

  private val runner = DefaultCoilixApplicationRunner(
    SimpleArgumentsParser(),
    ClasspathEnvironmentLoader("runner", ConfigFormat.byPrecedence, getClass.getClassLoader),
    components => StartupComponents.default(using components).copy(shutdownHooks = registrar)
  )

  private val context = runner.run[FixtureApplication.type](List("--name=Ann", "input.txt"))

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

  test("runs the runners after startup") {
    assertEquals(context.getBean[RunJournal].recorded, List("first", "second", "unordered:Ann"))
  }

  test("registers a shutdown hook for the started context") {
    assert(hooked.get.exists(_ eq context))
  }

  test("returns an active context") {
    assert(context.isActive)
  }
}
