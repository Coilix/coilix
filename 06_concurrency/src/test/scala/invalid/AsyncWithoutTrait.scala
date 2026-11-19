package dev.coilix.concurrency
package invalid

import dev.coilix.annotation.Async
import dev.coilix.annotation.Component

@Component
final class AsyncWithoutTrait {
  @Async
  def run(): Unit = ()
}
