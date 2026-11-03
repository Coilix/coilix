package dev.coilix.demo

import dev.coilix.annotation.Repository

/** A [[dev.coilix.demo.UserRepository]] with three fixed users, in unsorted order. */
@Repository
final class InMemoryUserRepository extends UserRepository {
  private val users = List("Charlie", "Alice", "Bob")

  override def findAll(): List[String] = users
}
