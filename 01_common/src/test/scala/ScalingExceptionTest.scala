package org.scaling.common

import munit.FunSuite

final class ScalingExceptionTest extends FunSuite {

  test("keeps the message") {
    assertEquals(ScalingException("boom").getMessage, "boom")
  }

  test("is a runtime exception") {
    assert(classOf[RuntimeException].isInstance(ScalingException("boom")))
  }
}
