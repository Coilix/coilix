package org.scaling.demo

import org.scaling.annotation.Primary
import org.scaling.annotation.Service

@Service
@Primary
final class SortedUserService(repository: UserRepository) extends UserService {
  override def listUsers(): List[String] = repository.findAll().sorted
}
