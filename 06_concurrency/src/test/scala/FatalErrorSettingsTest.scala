package dev.coilix.concurrency

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ComponentProvider
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.context.ApplicationContextBuilder
import dev.coilix.context.BeanDefinitionFactory
import dev.coilix.context.RegisteredBean
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class FatalErrorSettingsTest extends FunSuite {
  private given ComponentProvider = ComponentProvider.builtIn

  test("shuts down with exit code 70 within 15 seconds by default") {
    assertEquals(FatalErrorSettings.from(Environment.empty), FatalErrorSettings(FatalErrorAction.Shutdown, 70, 15.seconds))
  }

  test("reads the action, the exit code and the shutdown timeout") {
    assertEquals(
      FatalErrorSettings.from(environment("coilix.fatal-errors { action = log, exit-code = 3, shutdown-timeout = 2s }")),
      FatalErrorSettings(FatalErrorAction.Log, 3, 2.seconds)
    )
  }

  test("the module registers the shutdown handler by default and the logging one for action log") {
    assertEquals(
      (handlerClass(""), handlerClass("coilix.fatal-errors.action = LOG")),
      (classOf[ShutdownFatalErrorHandler], classOf[LoggingFatalErrorHandler])
    )
  }

  private def handlerClass(config: String): Class[?] = {
    val context = ApplicationContextBuilder.default.build(
      ConcurrencyModule().components.flatMap(BeanDefinitionFactory.default.create),
      List(RegisteredBean.of[Environment]("environment", environment(config)))
    )
    try {
      context.getBean[FatalErrorHandler].getClass
    } finally {
      context.close()
    }
  }

  private def environment(config: String): Environment =
    Environment.of(TypesafeConfigTree(ConfigFactory.parseString(config)))
}
