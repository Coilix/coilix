package dev.coilix.processing
package fleet

import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Value
import dev.coilix.processing.annotation.Processor
import scala.annotation.tailrec

/** Works on each shard until it stops and records what happens to it. */
@Processor
final class Ticker(journal: Journal, @Value("${fleet.greeting:tick}") greeting: String) extends BlockingProcessor {

  override val id: TestProcessor = TestProcessor.Ticker

  override def defaultShards: Int = 2

  override def prepare(shard: Shard): Unit = journal.add(s"prepare $shard")

  override def onAcquired(shard: Shard): Unit = journal.add(s"acquired $shard")

  override def onReleased(shard: Shard, reason: ReleaseReason): Unit = journal.add(s"released $shard $reason")

  override def run(shard: Shard): Unit = {
    journal.add(s"$greeting $shard on ${Thread.currentThread.getName}")
    idle(shard)
  }

  @PreDestroy
  def close(): Unit = journal.add("closed ticker")

  @tailrec
  private def idle(shard: Shard): Unit =
    if (!shard.stopRequested) {
      Thread.sleep(2)
      idle(shard)
    }
}
