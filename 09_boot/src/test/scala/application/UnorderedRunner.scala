package io.github.coilix.boot.application

import io.github.coilix.annotation.Component
import io.github.coilix.boot.ApplicationArguments
import io.github.coilix.boot.ApplicationRunner

@Component
final class UnorderedRunner(journal: RunJournal) extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit =
    journal.record(s"unordered:${arguments.optionValue("name").getOrElse("")}")
}
