package dev.coilix.context
package scopes

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Prototype

@Component
@Prototype
final class ShoppingCart(journal: CreationJournal) {
  journal.record("cart created")

  @PreDestroy
  def destroy(): Unit = journal.record("cart destroyed")
}
