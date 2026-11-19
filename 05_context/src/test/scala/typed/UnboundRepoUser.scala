package dev.coilix.context
package typed

import dev.coilix.annotation.Component

@Component
final class UnboundRepoUser[F[_]](repo: Repo[F]) {
  def name: String = repo.name
}
