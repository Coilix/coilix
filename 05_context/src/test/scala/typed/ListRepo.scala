package dev.coilix.context
package typed

import dev.coilix.annotation.Component

@Component
final class ListRepo extends Repo[List] {
  override def name: String = "list"
}
