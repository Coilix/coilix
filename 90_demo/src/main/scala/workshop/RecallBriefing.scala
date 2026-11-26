package dev.coilix.demo
package workshop

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.annotation.Slf4s
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.IOApplicationRunner
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reads the notice on the [[dev.coilix.demo.workshop.RecallBoard]] to the staff before the shift starts.
 *
 * It is an `IOApplicationRunner`, so it runs in one list with the other runners, and `@Order(0)` puts it before the
 * [[dev.coilix.demo.workshop.WorkshopShift]].
 */
@Component
@Order(0)
@Slf4s
final class RecallBriefing(board: RecallBoard) extends IOApplicationRunner {
  private val done = AtomicBoolean(false)

  /** Whether the briefing took place. */
  def isDone: Boolean = done.get

  override def run(arguments: ApplicationArguments): IO[Unit] =
    IO(board.pinned.getOrElse("No recall notices")).flatMap(notice => logF.info(s"Morning briefing: $notice")) *>
      IO(done.set(true))
}
