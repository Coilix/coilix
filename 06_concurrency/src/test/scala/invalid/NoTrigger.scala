package dev.coilix.concurrency
package invalid

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled

@Component
final class NoTrigger {
  @Scheduled
  def run(): IO[Unit] = IO.unit
}
