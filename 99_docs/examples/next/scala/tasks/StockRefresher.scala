package com.example
package tasks

import cats.effect.FiberIO
import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all.*
import dev.coilix.annotation.Component
import dev.coilix.context.ApplicationContext
import dev.coilix.context.ClosingPhase
import dev.coilix.context.ContextLifecycleListener
import fs2.Stream
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.DurationInt

@Component
final class StockRefresher extends ContextLifecycleListener {
  private val counter = AtomicInteger()
  private val running = Ref.unsafe[IO, Option[FiberIO[Unit]]](None)

  def refreshes: Int = counter.get

  override def closingPhase: ClosingPhase = ClosingPhase.Producers

  override def onContextStarted(context: ApplicationContext): IO[Unit] =
    Stream.fixedDelay[IO](50.millis).evalMap(_ => IO(counter.incrementAndGet())).compile.drain.start.flatMap(fiber =>
      running.set(Some(fiber))
    )

  override def onContextClosing: IO[Unit] = running.getAndSet(None).flatMap(_.traverse_(_.cancel))
}
