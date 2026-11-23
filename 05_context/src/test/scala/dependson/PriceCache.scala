package dev.coilix.context
package dependson

import dev.coilix.annotation.Component
import dev.coilix.annotation.DependsOn
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy

@Component
@DependsOn(Array("schemaMigrations"))
final class PriceCache(journal: StartupJournal) {
  @PostConstruct
  def load(): Unit = journal.record("prices loaded")

  @PreDestroy
  def close(): Unit = journal.record("prices dropped")
}
