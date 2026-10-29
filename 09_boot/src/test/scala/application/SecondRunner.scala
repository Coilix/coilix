package io.github.coilix.boot.application

import io.github.coilix.annotation.Component
import io.github.coilix.annotation.Order
import io.github.coilix.boot.ApplicationArguments
import io.github.coilix.boot.ApplicationRunner

@Component
@Order(2)
final class SecondRunner(journal: RunJournal) extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit = journal.record("second")
}
