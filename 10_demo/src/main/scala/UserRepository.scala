package dev.coilix.demo

/** Loads users. */
trait UserRepository {

  /** Names of all users. */
  def findAll(): List[String]
}
