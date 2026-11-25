package dev.coilix.testkit
package effects
package warmup

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.IOApplicationRunner

@Component
final class AuditRunner(journal: WarmupJournal) extends IOApplicationRunner {
  override def run(arguments: ApplicationArguments): IO[Unit] = IO(journal.record("audit"))
}
