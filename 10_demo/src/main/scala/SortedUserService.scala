package dev.coilix.demo

import dev.coilix.annotation.Primary
import dev.coilix.annotation.Service

/** Returns users sorted by name. Marked with `@Primary`, so it is injected where one [[dev.coilix.demo.UserService]] is needed. */
@Service
@Primary
final class SortedUserService(repository: UserRepository) extends UserService {
  override def listUsers(): List[String] = repository.findAll().sorted
}
