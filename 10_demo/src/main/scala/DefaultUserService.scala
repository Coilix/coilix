package org.scaling.demo

import org.scaling.annotation.Service

@Service
final class DefaultUserService(repository: UserRepository) extends UserService {
  override def listUsers(): List[String] = repository.findAll()
}
