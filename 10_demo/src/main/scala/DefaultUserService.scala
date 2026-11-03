package dev.coilix.demo

import dev.coilix.annotation.Service

/**
 * Returns users in the order the repository gives them.
 *
 * It is one of two [[dev.coilix.demo.UserService]] beans. Because [[dev.coilix.demo.SortedUserService]] is marked with `@Primary`,
 * this one is only
 * seen through `List[UserService]`.
 */
@Service
final class DefaultUserService(repository: UserRepository) extends UserService {
  override def listUsers(): List[String] = repository.findAll()
}
