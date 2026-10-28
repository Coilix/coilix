package org.scaling.demo

import org.scaling.annotation.Repository

@Repository
final class InMemoryUserRepository extends UserRepository {
  private val users = List("Charlie", "Alice", "Bob")

  override def findAll(): List[String] = users
}
