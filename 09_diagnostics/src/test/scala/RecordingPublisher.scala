package dev.coilix.diagnostics

import cats.data.EitherT
import cats.effect.IO
import dev.coilix.common.TypeInfo
import dev.coilix.events.EventPublisher
import dev.coilix.events.PublishError
import java.util.concurrent.atomic.AtomicReference

/** An [[dev.coilix.events.EventPublisher]] that keeps the published events. */
final class RecordingPublisher extends EventPublisher {

  private val published = AtomicReference(List.empty[Any])

  override def tryPublish[E: TypeInfo](event: E): EitherT[IO, PublishError, Unit] =
    EitherT.right(IO(published.updateAndGet(_ :+ event)).void)

  /** The published events, oldest first. */
  def events: List[Any] = published.get
}
