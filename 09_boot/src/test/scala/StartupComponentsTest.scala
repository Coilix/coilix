package org.scaling.boot

import com.typesafe.config.ConfigFactory
import munit.FunSuite
import org.scaling.config.ComponentProvider
import org.scaling.config.Environment
import org.scaling.config.TypesafeConfigTree

final class StartupComponentsTest extends FunSuite {

  private def provider(hocon: String): ComponentProvider =
    ComponentProvider.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon))))

  test("uses built-in components without configuration") {
    val components = StartupComponents.default(using ComponentProvider.builtIn)
    assertEquals(
      List(components.resolver, components.invoker, components.startupLogger, components.shutdownHooks).map(_.getClass),
      List(
        classOf[AnnotationBasePackageResolver],
        classOf[OrderedRunnerInvoker],
        classOf[Slf4jStartupLogger],
        classOf[RuntimeShutdownHookRegistrar]
      )
    )
  }

  test("replaces a nested component from configuration") {
    val configured = provider(
      """scaling.boot.startup-message-formatter {
        |  class = org.scaling.boot.overrides.TaggedStartupMessageFormatter
        |  tag = "[app]"
        |  delegate.class = org.scaling.boot.DefaultStartupMessageFormatter
        |}""".stripMargin
    )
    assertEquals(StartupMessageFormatter.default(using configured).starting("Demo"), "[app] Starting Demo")
  }
}
