package dev.coilix.demo

import dev.coilix.annotation.Repository

@Repository
final class InMemoryUserRepository extends UserRepository {
  private val users = List("Charlie", "Alice", "Bob")

  override def findAll(): List[String] = users
}
