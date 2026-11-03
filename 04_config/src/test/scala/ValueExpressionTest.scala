package dev.coilix.config

import munit.FunSuite

final class ValueExpressionTest extends FunSuite {

  test("parses a placeholder") {
    assertEquals(ValueExpression.parse("${app.name}"), ValueExpression.Placeholder("app.name", None))
  }

  test("parses a placeholder with a default") {
    assertEquals(ValueExpression.parse("${app.port:8080}"), ValueExpression.Placeholder("app.port", Some("8080")))
  }

  test("accepts an empty default") {
    assertEquals(ValueExpression.parse("${app.name:}"), ValueExpression.Placeholder("app.name", Some("")))
  }

  test("treats other text as a literal") {
    assertEquals(ValueExpression.parse("plain"), ValueExpression.Literal("plain"))
  }
}
