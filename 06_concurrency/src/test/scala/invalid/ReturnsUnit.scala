package dev.coilix.concurrency
package invalid

import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled

@Component
final class ReturnsUnit {
  @Scheduled(fixedRate = "1s")
  def run(): Unit = ()
}
