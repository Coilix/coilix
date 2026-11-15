package dev.coilix.common
package validation

class ViolationTest extends munit.FunSuite {

  private val blankName = Violation("Name must not be blank")

  private lazy val tooManyHTTPRedirects = Violation("Too many redirects")

  test("takes the code from the name of the val") {
    assertEquals(blankName, Violation("blank.name", "Name must not be blank"))
    assertEquals(tooManyHTTPRedirects.code, "too.many.http.redirects")
  }

  test("withPath sets the field") {
    assertEquals(blankName.withPath("name").path, Some("name"))
  }

  test("under adds a prefix to the path") {
    assertEquals(blankName.under("user").path, Some("user"))
    assertEquals(blankName.withPath("name").under("user").path, Some("user.name"))
  }

  test("toString shows the path before the message") {
    assertEquals(blankName.toString, "Name must not be blank")
    assertEquals(blankName.withPath("name").toString, "name: Name must not be blank")
  }

  test("compilation fails for an empty message or a message with %") {
    assert(compileErrors("""val empty = Violation("")""").contains("must not be empty"))
    assert(compileErrors("""val formatted = Violation("Got %s")""").contains("use ViolationTemplate"))
  }

  test("compilation fails for a message that is not a literal") {
    val text = "x"
    assert(compileErrors("""val dynamic = Violation(text)""").contains("must be a string literal"))
    assertEquals(text, "x")
  }

  test("compilation fails when the violation is not assigned to a val") {
    assert(compileErrors("""def notVal = Violation("Wrong")""").contains("must be assigned directly to a val"))
  }
}
