package dev.coilix.config

import munit.FunSuite

final class PropertyNamesTest extends FunSuite {

  test("converts camel case to kebab case") {
    assertEquals(PropertyNames.kebabCase("maxPoolSize"), "max-pool-size")
  }

  test("tries the exact name first") {
    assertEquals(PropertyNames.candidates("useTls"), List("useTls", "use-tls"))
  }

  test("does not repeat lower case names") {
    assertEquals(PropertyNames.candidates("host"), List("host"))
  }
}
