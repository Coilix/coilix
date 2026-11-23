package dev.coilix.context
package dependson

import dev.coilix.annotation.Component
import dev.coilix.annotation.Lazy
import dev.coilix.annotation.PostConstruct

@Component
@Lazy
final class AuditTrail(journal: StartupJournal) {
  @PostConstruct
  def open(): Unit = journal.record("audit opened")
}
