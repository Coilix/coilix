package dev.coilix.context
package dependson

import dev.coilix.annotation.Component
import dev.coilix.annotation.DependsOn
import dev.coilix.annotation.Lazy
import dev.coilix.annotation.PostConstruct

@Component
@Lazy
@DependsOn(Array("auditTrail"))
final class LedgerCache(journal: StartupJournal) {
  @PostConstruct
  def load(): Unit = journal.record("ledger loaded")
}
