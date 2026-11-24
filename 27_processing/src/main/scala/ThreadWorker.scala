package dev.coilix.processing

import java.util.concurrent.ThreadFactory
import org.slf4j.MDC
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

/**
 * One attempt of the work on a shard on its own thread, named `coilix-processor-<name>-<shard>`.
 *
 * The thread runs [[dev.coilix.processing.ProcessorDefinition.prepare]] and then the work, with the name of the processor
 * and the index of the shard in the MDC under `processor` and `shard`. Stopping asks the shard to stop, waits half of
 * the timeout, interrupts the thread and waits the other half. An interruption while the shard is stopping counts as a
 * normal end of the work.
 */
final class ThreadWorker(
  definition: ProcessorDefinition,
  shard:      Shard,
  events:     WorkerEvents,
  threads:    ThreadFactory,
  work: Shard => Unit,
  onStop: ()  => Unit
) extends RunningWorker {

  private val thread = threads.newThread(() => runAttempt())

  /** Starts the thread and returns this worker. */
  def start(): ThreadWorker = {
    thread.setName(s"coilix-processor-${shard.processor.name}-${shard.index}")
    thread.start()
    this
  }

  override def stop(timeout: FiniteDuration): StopOutcome = {
    shard.requestStop()
    onStop()
    val half = timeout / 2
    if (joined(half) || interruptedAndJoined(timeout - half)) StopOutcome.Stopped else StopOutcome.Abandoned
  }

  private def interruptedAndJoined(timeout: FiniteDuration): Boolean = {
    thread.interrupt()
    joined(timeout)
  }

  private def joined(timeout: FiniteDuration): Boolean = {
    thread.join(math.max(timeout.toMillis, 1L))
    !thread.isAlive
  }

  private def runAttempt(): Unit = {
    MDC.put("processor", shard.processor.name)
    MDC.put("shard", shard.index.toString)
    failureOf(definition.prepare(shard)) match {
      case Some(error) => events.crashed(FailureStage.Prepare, error)
      case None        => runWork()
    }
  }

  private def runWork(): Unit = {
    events.prepared()
    failureOf(work(shard)) match {
      case Some(error) => events.crashed(FailureStage.Process, error)
      case None        => events.finished()
    }
  }

  private def failureOf(task: => Unit): Option[Throwable] =
    try {
      task
      None
    } catch {
      case error: InterruptedException => Option.when(!shard.stopRequested)(error)
      case NonFatal(error) => Option(error)
    }
}
