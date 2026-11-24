package dev.coilix.processing
package flaky

import dev.coilix.processing.annotation.Processor

/** Fails on every start. */
@Processor
final class Flaky(journal: Journal) extends BlockingProcessor {

  override val id: TestProcessor = TestProcessor.Flaky

  override def onReleased(shard: Shard, reason: ReleaseReason): Unit = journal.add(s"released $shard $reason")

  override def run(shard: Shard): Unit = {
    journal.add(s"attempt $shard")
    throw IllegalStateException("the warehouse is offline")
  }
}
