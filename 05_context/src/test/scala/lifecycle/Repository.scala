package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class Repository(connection: Connection, journal: Journal) {
  journal.record(s"repository created, connection open: ${connection.isOpen}")

  @PreDestroy
  def flush: Unit = journal.record("repository flushed")
}
