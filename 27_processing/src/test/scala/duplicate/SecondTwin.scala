package dev.coilix.processing
package duplicate

import dev.coilix.processing.annotation.Processor

/** A processor with the identifier of [[dev.coilix.processing.duplicate.FirstTwin]]. */
@Processor
final class SecondTwin extends BlockingProcessor {

  override val id: TestProcessor = TestProcessor.Twin

  override def run(shard: Shard): Unit = ()
}
