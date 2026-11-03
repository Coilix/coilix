package dev.coilix.boot
package application

import dev.coilix.annotation.Component
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner

@Component
final class UnorderedRunner(journal: RunJournal) extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit =
    journal.record(s"unordered:${arguments.optionValue("name").getOrElse("")}")
}
