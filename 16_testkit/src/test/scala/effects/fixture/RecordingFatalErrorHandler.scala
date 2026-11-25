package dev.coilix.testkit
package effects
package fixture

import dev.coilix.concurrency.FatalErrorHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

final class RecordingFatalErrorHandler extends FatalErrorHandler {
  private val handled = AtomicReference(List.empty[Throwable])

  val called: CountDownLatch = CountDownLatch(1)

  def failures: List[Throwable] = handled.get

  override def handle(thread: Thread, failure: Throwable): Unit = {
    val _ = handled.updateAndGet(_ :+ failure)
    called.countDown()
  }
}
