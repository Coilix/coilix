package dev.coilix.concurrency
package invalid

import dev.coilix.annotation.Component
import dev.coilix.annotation.Scheduled

@Component
final class NoTrigger {
  @Scheduled
  def run(): Unit = ()
}
