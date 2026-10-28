package org.scaling.boot

import munit.FunSuite

final class ApplicationArgumentsTest extends FunSuite {
  private val arguments = ApplicationArguments(
    List("--name=Ann", "--name=Bob", "--debug", "file.txt"),
    Map("name" -> List("Ann", "Bob"), "debug" -> Nil),
    List("file.txt")
  )

  test("lists option names") {
    assertEquals(arguments.optionNames, Set("name", "debug"))
  }

  test("checks whether an option is present") {
    assert(arguments.containsOption("debug"))
    assert(!arguments.containsOption("verbose"))
  }

  test("returns all values of an option") {
    assertEquals(arguments.optionValues("name"), List("Ann", "Bob"))
  }

  test("returns no values for a missing option") {
    assertEquals(arguments.optionValues("verbose"), Nil)
  }

  test("returns the first value of an option") {
    assertEquals(arguments.optionValue("name"), Some("Ann"))
  }

  test("has no arguments when empty") {
    assertEquals(ApplicationArguments.empty.sourceArgs, Nil)
  }
}
