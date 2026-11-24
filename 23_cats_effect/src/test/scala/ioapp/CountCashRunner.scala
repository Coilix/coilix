package dev.coilix.catseffect
package ioapp

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.boot.ApplicationArguments

@Component
@Order(1)
final class CountCashRunner(receipts: Receipts) extends IOApplicationRunner {
  override def run(arguments: ApplicationArguments): IO[Unit] =
    if (arguments.containsOption("fail")) IO.raiseError(IllegalStateException("the till does not add up"))
    else IO(IOAppJournal.record(s"counted ${receipts.openingCash}"))
}
