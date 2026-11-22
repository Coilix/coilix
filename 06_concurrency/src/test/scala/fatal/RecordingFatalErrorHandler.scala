package dev.coilix.concurrency
package fatal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

final class RecordingFatalErrorHandler extends FatalErrorHandler {
  private val handled = AtomicReference(List.empty[(String, Throwable)])

  val called: CountDownLatch = CountDownLatch(1)

  def failures: List[(String, Throwable)] = handled.get

  override def handle(thread: Thread, failure: Throwable): Unit = {
    val _ = handled.updateAndGet(_ :+ (thread.getName, failure))
    called.countDown()
  }
}
