package dev.coilix.common
package validation

class ViolationTest extends munit.FunSuite {

  private val blank = Violation("name.blank", "Name must not be blank")

  test("withPath sets the field") {
    assertEquals(blank.withPath("name").path, Some("name"))
  }

  test("under adds a prefix to the path") {
    assertEquals(blank.under("user").path, Some("user"))
    assertEquals(blank.withPath("name").under("user").path, Some("user.name"))
  }

  test("toString shows the path before the message") {
    assertEquals(blank.toString, "Name must not be blank")
    assertEquals(blank.withPath("name").toString, "name: Name must not be blank")
  }
}
