package dev.coilix.concurrency
package fatal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

final class RecordingTerminator extends ProcessTerminator {
  private val calls = AtomicReference(List.empty[String])

  val exited: CountDownLatch = CountDownLatch(1)

  val halted: CountDownLatch = CountDownLatch(1)

  def recorded: List[String] = calls.get

  override def exit(code: Int): Unit = {
    val _ = calls.updateAndGet(_ :+ s"exit $code")
    exited.countDown()
  }

  override def halt(code: Int): Unit = {
    val _ = calls.updateAndGet(_ :+ s"halt $code")
    halted.countDown()
  }
}
