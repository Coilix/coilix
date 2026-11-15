package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class BrokenShutdown(repository: Repository, journal: Journal) {

  @PreDestroy
  def fail(): Unit = throw IllegalStateException(s"cannot stop with $repository")

  @PreDestroy
  def report(): Unit = journal.record("broken shutdown reported")
}
