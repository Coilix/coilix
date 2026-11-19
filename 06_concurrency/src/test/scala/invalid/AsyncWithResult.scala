package dev.coilix.concurrency
package invalid

import dev.coilix.annotation.Async
import dev.coilix.annotation.Component

@Component
final class AsyncWithResult extends Counter {
  @Async
  override def count(): Int = 1
}
