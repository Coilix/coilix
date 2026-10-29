package io.github.coilix.common

import munit.FunSuite

final class CoilixExceptionTest extends FunSuite {

  test("keeps the message") {
    assertEquals(CoilixException("boom").getMessage, "boom")
  }

  test("is a runtime exception") {
    assert(classOf[RuntimeException].isInstance(CoilixException("boom")))
  }
}
