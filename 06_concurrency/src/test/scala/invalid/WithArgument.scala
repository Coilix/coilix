package dev.coilix.concurrency
package invalid

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled

@Component
final class WithArgument {
  @Scheduled(fixedRate = "1s")
  def run(times: Int): IO[Unit] = IO.unit
}
