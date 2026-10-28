package org.scaling.demo

final class FixedUserRepository(users: List[String]) extends UserRepository {
  override def findAll(): List[String] = users
}
