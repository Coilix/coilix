package org.scaling.demo

trait UserRepository {
  def findAll(): List[String]
}
