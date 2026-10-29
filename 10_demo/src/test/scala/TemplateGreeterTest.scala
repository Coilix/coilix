package io.github.coilix.demo

import munit.FunSuite

final class TemplateGreeterTest extends FunSuite {

  test("greets with the configured word") {
    assertEquals(TemplateGreeter("Hey").greet("Ann"), "Hey, Ann")
  }
}
