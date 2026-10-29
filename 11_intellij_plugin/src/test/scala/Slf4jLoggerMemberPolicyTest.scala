package io.github.coilix.intellij

import munit.FunSuite

final class Slf4jLoggerMemberPolicyTest extends FunSuite {
  private val policy = LoggerMemberPolicy.default

  test("adds a logger to an annotated definition") {
    assertEquals(policy.loggerMembers(List("Component", "Slf4s"), List("run")), List("def log: _root_.org.slf4j.Logger = ???"))
  }

  test("adds nothing to a definition without the annotation") {
    assertEquals(policy.loggerMembers(List("Component"), List("run")), Nil)
  }

  test("keeps an explicitly declared log member") {
    assertEquals(policy.loggerMembers(List("Slf4s"), List("log")), Nil)
  }
}
