package dev.coilix.processing
package misuse

import cats.effect.IO
import dev.coilix.processing.annotation.Processor

/** A processor that [[dev.coilix.processing.misuse.Supervisor]] wrongly injects. */
@Processor
final class Worker extends IOProcessor {

  override val id: TestProcessor = TestProcessor.Ticker

  override def run(shard: Shard): IO[Unit] = IO.unit
}
