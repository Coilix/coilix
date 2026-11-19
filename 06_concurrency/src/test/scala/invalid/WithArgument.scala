package dev.coilix.concurrency
package invalid

import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled

@Component
final class WithArgument {
  @Scheduled(fixedRate = "1s")
  def run(times: Int): Unit = ()
}
