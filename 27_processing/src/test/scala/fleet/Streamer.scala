package dev.coilix.processing
package fleet

import cats.effect.IO
import dev.coilix.processing.annotation.Processor

/** Streams until its shard is asked to stop and then finishes its current step. */
@Processor
final class Streamer(journal: Journal) extends IOProcessor {
  override val id: TestProcessor = TestProcessor.Streamer

  override def run(shard: Shard): IO[Unit] =
    IO(journal.add(s"streaming $shard")) *> shard.stopped *> IO(journal.add(s"stream stopped $shard"))
}
