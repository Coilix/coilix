package dev.coilix.context
package typed

import dev.coilix.annotation.Component

@Component
final class LambdaRepoUser(repo: Repo[[A] =>> Either[String, A]]) {
  def name: String = repo.name
}
