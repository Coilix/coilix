package io.github.coilix.demo

trait UserRepository {
  def findAll(): List[String]
}
