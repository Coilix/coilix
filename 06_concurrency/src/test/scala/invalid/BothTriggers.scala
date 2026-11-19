package dev.coilix.concurrency
package invalid

import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled

@Component
final class BothTriggers {
  @Scheduled(fixedRate = "1s", fixedDelay = "1s")
  def run(): Unit = ()
}
