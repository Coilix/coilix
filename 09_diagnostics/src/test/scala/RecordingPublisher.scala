package dev.coilix.diagnostics

import dev.coilix.common.TypeInfo
import dev.coilix.events.EventPublisher
import java.util.concurrent.atomic.AtomicReference

/** An [[dev.coilix.events.EventPublisher]] that keeps the published events. */
final class RecordingPublisher extends EventPublisher {

  private val published = AtomicReference(List.empty[Any])

  override def publish[E: TypeInfo](event: E): Unit = {
    val _ = published.updateAndGet(_ :+ event)
  }

  /** The published events, oldest first. */
  def events: List[Any] = published.get
}
