package dev.coilix.intellij

import munit.FunSuite

final class SimpleNameSlf4sAnnotationMatcherTest extends FunSuite {
  private val matcher = new SimpleNameSlf4sAnnotationMatcher

  test("matches the simple annotation name") {
    assert(matcher.matches("Slf4s"))
  }

  test("matches the fully qualified annotation name") {
    assert(matcher.matches("dev.coilix.annotation.Slf4s"))
  }

  test("matches the root qualified annotation name") {
    assert(matcher.matches("_root_.dev.coilix.annotation.Slf4s"))
  }

  test("ignores other annotations") {
    assert(!matcher.matches("dev.coilix.annotation.Component"))
  }

  test("ignores annotations that only contain the name") {
    assert(!matcher.matches("Slf4sLike"))
  }
}
