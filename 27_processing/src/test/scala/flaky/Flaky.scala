package dev.coilix.processing
package flaky

import cats.effect.IO
import dev.coilix.processing.annotation.Processor

/** Fails on every start. */
@Processor
final class Flaky(journal: Journal) extends IOProcessor {

  override val id: TestProcessor = TestProcessor.Flaky

  override def onReleased(shard: Shard, reason: ReleaseReason): IO[Unit] = IO(journal.add(s"released $shard $reason"))

  override def run(shard: Shard): IO[Unit] =
    IO(journal.add(s"attempt $shard")) *> IO.raiseError(IllegalStateException("the warehouse is offline"))
}
