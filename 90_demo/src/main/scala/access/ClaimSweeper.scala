package dev.coilix.demo
package access

import cats.effect.FiberIO
import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all.*
import dev.coilix.annotation.Component
import dev.coilix.annotation.Slf4s
import dev.coilix.annotation.Value
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import fs2.Stream
import scala.concurrent.duration.FiniteDuration

/**
 * Makes the [[dev.coilix.demo.access.InMemoryReplayGuard]] forget old requests every `workshop.replay-window`.
 *
 * The window comes from the configuration, so the sweeping is a stream of fs2 with a fixed delay, run on a fiber that starts
 * with the context. The sweeping brings new work in, so the fiber is cancelled in the
 * [[dev.coilix.context.ClosingPhase.Producers]] phase, together with the `@Scheduled` methods.
 */
@Component
@Slf4s
final class ClaimSweeper(guard: InMemoryReplayGuard, @Value("${workshop.replay-window:10m}") window: FiniteDuration)
    extends ContextLifecycleListener {
  private val sweeping = Ref.unsafe[IO, Option[FiberIO[Unit]]](None)

  /** Whether the sweeping still runs. */
  def isSweeping: IO[Boolean] = sweeping.get.map(_.isDefined)

  override def closingPhase: ClosingPhase = ClosingPhase.Producers

  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    Stream
      .fixedDelay[IO](window)
      .evalMap(_ => IO(guard.forget()).flatMap(forgotten => logF.info(s"Replay guard forgot $forgotten requests")))
      .compile
      .drain
      .start
      .flatMap(fiber => sweeping.set(Some(fiber)))

  override def onContextClosing: IO[Unit] =
    sweeping.getAndSet(None).flatMap(_.traverse_(_.cancel))
}
