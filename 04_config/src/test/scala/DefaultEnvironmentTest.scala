package dev.coilix.config

import munit.FunSuite

final class DefaultEnvironmentTest extends FunSuite {
  private val environment = DefaultEnvironment(ConfigTrees.parse("app { name = demo, port = 8080 }"), ConfigBinder.default)

  test("checks whether a path exists") {
    assert(environment.contains("app.port"))
  }

  test("reads a typed value") {
    assertEquals(environment.get[Int]("app.port"), Some(8080))
  }

  test("returns none for a missing value") {
    assertEquals(environment.get[String]("app.missing"), None)
  }

  test("resolves a placeholder") {
    assertEquals(environment.resolve(ValueExpression.parse("${app.name}"), classOf[String]), "demo")
  }

  test("uses the placeholder default when the key is missing") {
    assertEquals(environment.resolve(ValueExpression.parse("${app.timeout:30}"), classOf[Int]), Int.box(30))
  }

  test("prefers the configured value over the default") {
    assertEquals(environment.resolve(ValueExpression.parse("${app.port:1}"), classOf[Int]), Int.box(8080))
  }

  test("converts a literal") {
    assertEquals(environment.resolve(ValueExpression.parse("42"), classOf[Long]), Long.box(42L))
  }

  test("fails for a missing placeholder without a default") {
    intercept[ConfigurationException](environment.resolve(ValueExpression.parse("${app.missing}"), classOf[String]))
  }

  test("is empty by default") {
    assert(!Environment.empty.contains("app"))
  }
}
