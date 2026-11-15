package dev.coilix.common
package validation

class StatementTest extends munit.FunSuite {

  private val positive = Statement[Int](_ > 0, Violation("positive", "Must be positive"))

  test("holds checks the condition") {
    assert(positive.holds(1))
    assert(!positive.holds(0))
  }

  test("contramap checks the condition on a part of a larger value") {
    val length = positive.contramap[String](_.length)
    assert(length.holds("a"))
    assert(!length.holds(""))
    assertEquals(length.violation, positive.violation)
  }
}
