package dev.coilix.context
package lifecycle

import dev.coilix.annotation.PostConstruct

abstract class Startable(journal: Journal) {

  @PostConstruct
  private def announce(): Unit = journal.record(s"${getClass.getSimpleName} started")
}
