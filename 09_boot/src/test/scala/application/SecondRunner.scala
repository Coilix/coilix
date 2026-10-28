package org.scaling.boot.application

import org.scaling.annotation.Component
import org.scaling.annotation.Order
import org.scaling.boot.ApplicationArguments
import org.scaling.boot.ApplicationRunner

@Component
@Order(2)
final class SecondRunner(journal: RunJournal) extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit = journal.record("second")
}
