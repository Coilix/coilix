package dev.coilix.testkit
package effects
package repo

import dev.coilix.annotation.Component

@Component
final class OptionRepo extends Repo[Option] {
  override def find(id: Int): Option[Option[String]] = Some(None)
}
