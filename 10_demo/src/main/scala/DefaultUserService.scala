package dev.coilix.demo

import dev.coilix.annotation.Service

@Service
final class DefaultUserService(repository: UserRepository) extends UserService {
  override def listUsers(): List[String] = repository.findAll()
}
