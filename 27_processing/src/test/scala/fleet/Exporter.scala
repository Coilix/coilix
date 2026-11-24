package dev.coilix.processing
package fleet

import dev.coilix.processing.annotation.Processor

/** Exports the slots of its shard once and ends. */
@Processor
final class Exporter(journal: Journal) extends BlockingProcessor {

  override val id: TestProcessor = TestProcessor.Exporter

  override def onReleased(shard: Shard, reason: ReleaseReason): Unit = journal.add(s"released $shard $reason")

  override def run(shard: Shard): Unit = journal.add(s"exported ${shard.slots.size} slots of $shard")
}
