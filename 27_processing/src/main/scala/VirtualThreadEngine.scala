package dev.coilix.processing

import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.concurrency.FatalErrors
import dev.coilix.concurrency.ThreadFactories
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * The engine of the `processing` module: each shard works on its own virtual thread, or on a platform thread before
 * JDK 21.
 *
 * It runs [[dev.coilix.processing.BlockingProcessor]], [[dev.coilix.processing.CustomProcessor]] and
 * [[dev.coilix.processing.StatelessProcessor]]. A fatal error on a
 * worker thread goes to the [[dev.coilix.concurrency.FatalErrorHandler]].
 */
final class VirtualThreadEngine(fatalErrors: FatalErrorHandler) extends ProcessorEngine {

  private val threads: ThreadFactory =
    ThreadFactories.named("coilix-processor-", virtual = true, Option(FatalErrors.uncaughtExceptionHandler(fatalErrors)))

  override def name: String = "virtual-threads"

  override def supports(definition: ProcessorDefinition): Boolean =
    definition match {
      case _: BlockingProcessor | _: CustomProcessor | _: StatelessProcessor[?] => true
      case _                                                                    => false
    }

  override def start(definition: ProcessorDefinition, shard: Shard, events: WorkerEvents): RunningWorker =
    definition match {
      case blocking:  BlockingProcessor     => ThreadWorker(definition, shard, events, threads, blocking.run, () => ()).start()
      case custom:    CustomProcessor       => customWorker(custom, shard, events)
      case stateless: StatelessProcessor[?] =>
        ThreadWorker(definition, shard, events, threads, StatelessLoop(stateless).run, () => ()).start()
      case _ => throw IllegalArgumentException(s"$name does not run ${definition.getClass.getName}")
    }

  private def customWorker(custom: CustomProcessor, shard: Shard, events: WorkerEvents): RunningWorker = {
    val execution = AtomicReference(Option.empty[Execution])
    ThreadWorker(custom, shard, events, threads, runCustom(custom, execution), () => execution.get.foreach(_.stop())).start()
  }

  private def runCustom(custom: CustomProcessor, execution: AtomicReference[Option[Execution]])(shard: Shard): Unit = {
    val started = custom.run(shard)
    execution.set(Option(started))
    if (shard.stopRequested) started.stop()
    Await.result(started.completion, Duration.Inf)
  }
}
