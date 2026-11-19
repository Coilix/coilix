package dev.coilix.boot

import munit.FunSuite

final class SimpleArgumentsParserTest extends FunSuite {
  private val parser = SimpleArgumentsParser()

  test("parses an option with a value") {
    assertEquals(parser.parse(List("--name=Ann")).optionValue("name"), Some("Ann"))
  }

  test("collects repeated option values in order") {
    assertEquals(parser.parse(List("--name=Ann", "--name=Bob")).optionValues("name"), List("Ann", "Bob"))
  }

  test("parses an option without a value") {
    val arguments = parser.parse(List("--debug"))
    assert(arguments.containsOption("debug"))
    assertEquals(arguments.optionValues("debug"), Nil)
  }

  test("keeps everything after the first equals sign as the value") {
    assertEquals(parser.parse(List("--filter=a=b")).optionValue("filter"), Some("a=b"))
  }

  test("treats other arguments as positional") {
    assertEquals(parser.parse(List("in.txt", "--name=Ann", "-v", "--")).nonOptionArgs, List("in.txt", "-v", "--"))
  }

  test("keeps the source arguments") {
    assertEquals(parser.parse(List("a", "--b=c")).sourceArgs, List("a", "--b=c"))
  }
}
