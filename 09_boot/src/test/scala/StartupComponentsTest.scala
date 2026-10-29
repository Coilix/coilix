package io.github.coilix.boot

import com.typesafe.config.ConfigFactory
import io.github.coilix.config.ComponentProvider
import io.github.coilix.config.Environment
import io.github.coilix.config.TypesafeConfigTree
import munit.FunSuite

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
      """coilix.boot.startup-message-formatter {
        |  class = io.github.coilix.boot.overrides.TaggedStartupMessageFormatter
        |  tag = "[app]"
        |  delegate.class = io.github.coilix.boot.DefaultStartupMessageFormatter
        |}""".stripMargin
    )
    assertEquals(StartupMessageFormatter.default(using configured).starting("Demo"), "[app] Starting Demo")
  }
}
