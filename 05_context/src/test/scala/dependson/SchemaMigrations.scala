package dev.coilix.context
package dependson

import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy

@Component
final class SchemaMigrations(journal: StartupJournal) {
  @PostConstruct
  def migrate(): Unit = journal.record("migrated")

  @PreDestroy
  def close(): Unit = journal.record("migrations closed")
}
