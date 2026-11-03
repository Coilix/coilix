package dev.coilix.demo

/** Lists users. The demo has two implementations to show `@Primary` and `List[UserService]`. */
trait UserService {

  /** Names of all users. */
  def listUsers(): List[String]
}
