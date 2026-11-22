package com.example
package fatal

import dev.coilix.concurrency.ProcessTerminator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

final class RecordingTerminator extends ProcessTerminator {
  private val exitCodes = AtomicReference(List.empty[Int])

  val exited: CountDownLatch = CountDownLatch(1)

  def codes: List[Int] = exitCodes.get

  override def exit(code: Int): Unit = {
    val _ = exitCodes.updateAndGet(_ :+ code)
    exited.countDown()
  }

  override def halt(code: Int): Unit = exit(code)
}
