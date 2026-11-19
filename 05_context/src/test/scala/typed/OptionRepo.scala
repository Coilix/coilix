package dev.coilix.context
package typed

import dev.coilix.annotation.Component

@Component
final class OptionRepo extends Repo[Option] {
  override def name: String = "option"
}
