package dev.coilix.demo

import dev.coilix.annotation.Primary
import dev.coilix.annotation.Service

@Service
@Primary
final class SortedUserService(repository: UserRepository) extends UserService {
  override def listUsers(): List[String] = repository.findAll().sorted
}
