package io.github.coilix.demo

import io.github.coilix.annotation.Repository

@Repository
final class InMemoryUserRepository extends UserRepository {
  private val users = List("Charlie", "Alice", "Bob")

  override def findAll(): List[String] = users
}
