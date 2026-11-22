package dev.coilix.context
package scopes

import dev.coilix.annotation.Component
import dev.coilix.annotation.Lazy
import dev.coilix.annotation.PreDestroy

@Component
@Lazy
final class ReportArchive(journal: CreationJournal) {
  journal.record("archive created")

  @PreDestroy
  def close(): Unit = journal.record("archive closed")
}
