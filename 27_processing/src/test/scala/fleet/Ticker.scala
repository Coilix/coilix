package dev.coilix.processing
package fleet

import cats.effect.IO
import dev.coilix.annotation.PreDestroy
import dev.coilix.annotation.Value
import dev.coilix.processing.annotation.Processor

/** Works on each shard until it stops and records what happens to it. */
@Processor
final class Ticker(journal: Journal, @Value("${fleet.greeting:tick}") greeting: String) extends IOProcessor {
  override val id: TestProcessor = TestProcessor.Ticker

  override def defaultShards: Int = 2

  override def prepare(shard: Shard): IO[Unit] = IO(journal.add(s"prepare $shard"))

  override def onAcquired(shard: Shard): IO[Unit] = IO(journal.add(s"acquired $shard"))

  override def onReleased(shard: Shard, reason: ReleaseReason): IO[Unit] = IO(journal.add(s"released $shard $reason"))

  override def run(shard: Shard): IO[Unit] =
    IO(journal.add(s"$greeting $shard")) *> shard.stopped

  @PreDestroy
  def close(): Unit = journal.add("closed ticker")
}
