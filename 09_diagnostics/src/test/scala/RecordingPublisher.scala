package dev.coilix.diagnostics

import dev.coilix.common.TypeInfo
import dev.coilix.events.EventPublisher
import dev.coilix.events.PublishError
import java.util.concurrent.atomic.AtomicReference

/** An [[dev.coilix.events.EventPublisher]] that keeps the published events. */
final class RecordingPublisher extends EventPublisher {

  private val published = AtomicReference(List.empty[Any])

  override def tryPublish[E: TypeInfo](event: E): Either[PublishError, Unit] = {
    val _ = published.updateAndGet(_ :+ event)
    Right(())
  }

  /** The published events, oldest first. */
  def events: List[Any] = published.get
}
