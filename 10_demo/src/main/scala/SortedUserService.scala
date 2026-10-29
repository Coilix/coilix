package io.github.coilix.demo

import io.github.coilix.annotation.Primary
import io.github.coilix.annotation.Service

@Service
@Primary
final class SortedUserService(repository: UserRepository) extends UserService {
  override def listUsers(): List[String] = repository.findAll().sorted
}
