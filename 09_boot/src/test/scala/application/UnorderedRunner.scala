package org.scaling.boot.application

import org.scaling.annotation.Component
import org.scaling.boot.ApplicationArguments
import org.scaling.boot.ApplicationRunner

@Component
final class UnorderedRunner(journal: RunJournal) extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit =
    journal.record(s"unordered:${arguments.optionValue("name").getOrElse("")}")
}
