package dev.coilix.processing
package duplicate

import cats.effect.IO
import dev.coilix.processing.annotation.Processor

/** A processor with the identifier of [[dev.coilix.processing.duplicate.FirstTwin]]. */
@Processor
final class SecondTwin extends IOProcessor {

  override val id: TestProcessor = TestProcessor.Twin

  override def run(shard: Shard): IO[Unit] = IO.unit
}
