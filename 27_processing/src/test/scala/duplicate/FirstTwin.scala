package dev.coilix.processing
package duplicate

import cats.effect.IO
import dev.coilix.processing.annotation.Processor

/** A processor whose identifier is also used by [[dev.coilix.processing.duplicate.SecondTwin]]. */
@Processor
final class FirstTwin extends IOProcessor {

  override val id: TestProcessor = TestProcessor.Twin

  override def run(shard: Shard): IO[Unit] = IO.unit
}
