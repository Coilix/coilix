package io.github.coilix.context

import io.github.coilix.context.wiring.Farewell
import munit.FunSuite

final class NoSuchBeanExceptionTest extends FunSuite {
  test("names the missing bean type in the message") {
    assertEquals(NoSuchBeanException[Farewell].getMessage, s"No bean of type ${classOf[Farewell].getName}")
  }
}
