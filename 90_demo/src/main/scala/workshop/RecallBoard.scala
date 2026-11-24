package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Slf4s
import java.util.concurrent.atomic.AtomicReference

/**
 * The notice board of the workshop with the current recall bulletin, pinned when the workshop opens and taken down when it
 * closes.
 *
 * Pinning and taking down are `IO[Unit]` methods marked `@PostConstruct` and `@PreDestroy`, which the cats-effect module runs
 * when it creates and releases the bean.
 */
@Component
@Slf4s
final class RecallBoard(bulletin: RecallBulletin) {
  private val notice = AtomicReference(Option.empty[String])

  /** The notice on the board, or `None` when the board is empty. */
  def pinned: Option[String] = notice.get

  /** Pins the recall bulletin to the board. */
  @PostConstruct
  def pin(): IO[Unit] =
    IO(notice.set(Some(s"Recall bulletin ${bulletin.issue}"))) *> IO(log.info(s"Pinned recall bulletin ${bulletin.issue}"))

  /** Takes the bulletin down. */
  @PreDestroy
  def takeDown(): IO[Unit] =
    IO(notice.set(None)) *> IO(log.info(s"Took down recall bulletin ${bulletin.issue}"))
}
