package io.github.coilix.demo

import io.github.coilix.annotation.Service

@Service
final class DefaultUserService(repository: UserRepository) extends UserService {
  override def listUsers(): List[String] = repository.findAll()
}
