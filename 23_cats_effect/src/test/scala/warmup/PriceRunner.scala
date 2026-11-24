package dev.coilix.catseffect
package warmup

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.boot.ApplicationArguments

@Component
@Order(1)
final class PriceRunner(journal: WarmupJournal) extends IOApplicationRunner {
  override def run(arguments: ApplicationArguments): IO[Unit] =
    IO(journal.record(s"prices for ${arguments.nonOptionArgs.mkString}"))
}
