package dev.coilix.boot
package application

import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner

@Component
@Order(2)
final class SecondRunner(journal: RunJournal) extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit = journal.record("second")
}
