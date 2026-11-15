package dev.coilix.common
package validation

class ViolationTemplateTest extends munit.FunSuite {

  test("fills the placeholders in order") {
    val outside = ViolationTemplate[(Int, Int)]("range", "Value must be between %s and %s")(_._1.toString, _._2.toString)
    assertEquals(outside((1, 9)), Violation("range", "Value must be between 1 and 9", List("1", "9")))
    assertEquals(outside.code, "range")
    assertEquals(outside.template, "Value must be between %s and %s")
  }

  test("a template without placeholders needs no arguments") {
    assertEquals(ViolationTemplate[Int]("fixed", "Always wrong")().apply(1).message, "Always wrong")
  }

  test("rejects a different number of placeholders and arguments") {
    val error = intercept[IllegalArgumentException](ViolationTemplate[Int]("bad", "%s and %s")(_.toString))
    assert(error.getMessage.contains("has 2 placeholders, but 1 arguments"), error.getMessage)
  }

  test("rejects other format specifiers") {
    val error = intercept[IllegalArgumentException](ViolationTemplate[Int]("bad", "%d")(_.toString))
    assert(error.getMessage.contains("may contain only %s placeholders"), error.getMessage)
  }
}
