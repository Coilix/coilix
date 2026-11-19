package dev.coilix.context
package collected

import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Value
import dev.coilix.context.lifecycle.Journal
import java.beans.JavaBean

@JavaBean
final class CleanupTask(journal: Journal, @Value("${tasks.cleanup.target:temp}") target: String) extends Task {

  override def run(): String = s"cleaning $target"

  @PostConstruct
  def ready(): Unit = journal.record("cleanup ready")

  @PreDestroy
  def stop(): Unit = journal.record("cleanup stopped")
}
