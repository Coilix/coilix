package dev.coilix.common
package validation

class ViolationTemplateTest extends munit.FunSuite {

  private val valueOutOfRange =
    ViolationTemplate[(Int, Int)]("Value must be between %s and %s")(_._1.toString, _._2.toString)

  test("takes the code from the name of the val and fills the placeholders in order") {
    assertEquals(valueOutOfRange((1, 9)), Violation("value.out.of.range", "Value must be between 1 and 9", List("1", "9")))
    assertEquals(valueOutOfRange.code, "value.out.of.range")
    assertEquals(valueOutOfRange.template, "Value must be between %s and %s")
  }

  test("a template without placeholders needs no arguments") {
    val alwaysWrong = ViolationTemplate[Int]("Always wrong")()
    assertEquals(alwaysWrong(1), Violation("always.wrong", "Always wrong"))
  }

  test("the constructor sets the code explicitly") {
    assertEquals(new ViolationTemplate[Int]("custom", "Got %s", List(_.toString)).apply(3), Violation("custom", "Got 3", List("3")))
  }

  test("the constructor rejects a different number of placeholders and arguments") {
    val error = intercept[IllegalArgumentException](new ViolationTemplate[Int]("bad", "%s and %s", List(_.toString)))
    assert(error.getMessage.contains("has 2 placeholders, but 1 arguments"), error.getMessage)
  }

  test("the constructor rejects other format specifiers") {
    val error = intercept[IllegalArgumentException](new ViolationTemplate[Int]("bad", "%d", List(_.toString)))
    assert(error.getMessage.contains("may contain only %s placeholders"), error.getMessage)
  }

  test("compilation fails for a different number of placeholders and arguments") {
    val errors = compileErrors("""val bad = ViolationTemplate[Int]("%s and %s")(_.toString)""")
    assert(errors.contains("Violation template '%s and %s' has 2 placeholders, but 1 arguments"), errors)
  }

  test("compilation fails for other format specifiers") {
    assert(compileErrors("""val bad = ViolationTemplate[Int]("%d")(_.toString)""").contains("may contain only %s placeholders"))
  }

  test("compilation fails for a template that is not a literal") {
    val text = "x"
    assert(compileErrors("""val bad = ViolationTemplate[Int](text)()""").contains("must be a string literal"))
    assertEquals(text, "x")
  }

  test("compilation fails when the template is not assigned to a val") {
    assert(compileErrors("""def bad = ViolationTemplate[Int]("Wrong")()""").contains("must be assigned directly to a val"))
  }
}
