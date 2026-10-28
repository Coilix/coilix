package org.scaling.demo

import munit.FunSuite

final class InMemoryUserRepositoryTest extends FunSuite {

  test("returns the stored users") {
    assertEquals(InMemoryUserRepository().findAll(), List("Charlie", "Alice", "Bob"))
  }
}
