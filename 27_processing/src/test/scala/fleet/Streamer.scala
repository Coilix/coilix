package dev.coilix.processing
package fleet

import dev.coilix.processing.annotation.Processor
import scala.concurrent.Future
import scala.concurrent.Promise

/** Starts work that completes only when it is stopped. */
@Processor
final class Streamer(journal: Journal) extends CustomProcessor {

  override val id: TestProcessor = TestProcessor.Streamer

  override def run(shard: Shard): Execution = {
    journal.add(s"streaming $shard")
    val done = Promise[Unit]()
    new Execution {
      override def stop(): Unit = {
        journal.add(s"stream stopped $shard")
        done.trySuccess(()): Unit
      }

      override def completion: Future[Unit] = done.future
    }
  }
}
