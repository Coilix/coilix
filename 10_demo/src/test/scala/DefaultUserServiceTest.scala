package org.scaling.demo

import munit.FunSuite

final class DefaultUserServiceTest extends FunSuite {

  test("lists users in repository order") {
    assertEquals(DefaultUserService(FixedUserRepository(List("b", "a"))).listUsers(), List("b", "a"))
  }
}
