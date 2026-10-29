package io.github.coilix.config

import io.github.coilix.config.fixture.Greeting
import io.github.coilix.config.fixture.PlainGreeting
import munit.FunSuite

final class ConfigComponentProviderTest extends FunSuite {

  private def provider(hocon: String): ComponentProvider =
    ComponentProvider.from(Environment.of(ConfigTrees.parse(hocon)))

  test("returns the default when the path is not configured") {
    assertEquals(provider("").get[Greeting](PlainGreeting()).greet("Ann"), "Hi Ann")
  }

  test("builds the configured implementation instead of the default") {
    val configured = provider("test.greeting { class = io.github.coilix.config.fixture.PrefixGreeting, prefix = Hey }")
    assertEquals(configured.get[Greeting](fail("default must not be built")).greet("Ann"), "Hey Ann")
  }

  test("fails when the configured implementation cannot be loaded") {
    intercept[ConfigurationException](provider("test.greeting.class = com.example.Missing").get[Greeting](PlainGreeting()))
  }

  test("exposes its environment") {
    assert(provider("a = 1").environment.contains("a"))
  }

  test("has no overrides when built in") {
    assertEquals(ComponentProvider.builtIn.get[Greeting](PlainGreeting()).getClass, classOf[PlainGreeting])
  }

  test("config path carries the component type") {
    assertEquals(summon[ConfigPath[Greeting]], ConfigPath("test.greeting", classOf[Greeting]))
  }
}
