package org.scaling.demo

import munit.FunSuite

final class SortedUserServiceTest extends FunSuite {

  test("lists users sorted by name") {
    assertEquals(SortedUserService(FixedUserRepository(List("b", "a"))).listUsers(), List("a", "b"))
  }
}
