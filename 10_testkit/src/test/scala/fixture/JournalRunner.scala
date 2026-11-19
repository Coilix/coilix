package dev.coilix.testkit
package fixture

import dev.coilix.annotation.Component
import dev.coilix.annotation.Value
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.ApplicationRunner

@Component
final class JournalRunner(journal: Option[Journal], @Value("${fixture.fail-runner:false}") fail: Boolean)
    extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit = {
    journal.foreach(_.record("runner"))
    if (fail) throw IllegalStateException("runner failed")
  }
}
