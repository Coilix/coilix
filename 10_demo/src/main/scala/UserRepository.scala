package dev.coilix.demo

trait UserRepository {
  def findAll(): List[String]
}
