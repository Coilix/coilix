package dev.coilix.context
package typed

import dev.coilix.annotation.Component

@Component
final class RepoUser(optionRepo: Repo[Option], listRepo: Repo[List], vectorRepo: Option[Repo[Vector]], all: List[Repo[?]]) {
  def names: List[String] = List(optionRepo.name, listRepo.name) ++ vectorRepo.map(_.name)

  def count: Int = all.size
}
