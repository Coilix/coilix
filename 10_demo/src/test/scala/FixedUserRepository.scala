package io.github.coilix.demo

final class FixedUserRepository(users: List[String]) extends UserRepository {
  override def findAll(): List[String] = users
}
