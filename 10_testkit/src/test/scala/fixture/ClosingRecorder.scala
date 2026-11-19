package dev.coilix.testkit
package fixture

import dev.coilix.annotation.Component
import dev.coilix.annotation.PreDestroy

@Component
final class ClosingRecorder(journal: Option[Journal]) {

  @PreDestroy
  private def closing(): Unit = journal.foreach(_.record("closed"))
}
