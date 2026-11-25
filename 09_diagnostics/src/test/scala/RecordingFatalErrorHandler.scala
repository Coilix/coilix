package dev.coilix.diagnostics

import dev.coilix.concurrency.FatalErrorHandler
import java.util.concurrent.atomic.AtomicReference

/** A [[dev.coilix.concurrency.FatalErrorHandler]] that keeps what it was given. */
final class RecordingFatalErrorHandler extends FatalErrorHandler {

  private val handled = AtomicReference(List.empty[Throwable])

  override def handle(thread: Thread, failure: Throwable): Unit = {
    val _ = handled.updateAndGet(_ :+ failure)
  }

  /** The handled failures, oldest first. */
  def failures: List[Throwable] = handled.get
}
