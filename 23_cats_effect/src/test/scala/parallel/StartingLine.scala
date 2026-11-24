package dev.coilix.catseffect
package parallel

import dev.coilix.annotation.Component
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

@Component
final class StartingLine {
  private val barrier = CyclicBarrier(2)

  def await(): Unit = {
    val _ = barrier.await(5, TimeUnit.SECONDS)
  }
}
