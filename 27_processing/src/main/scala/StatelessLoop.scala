package dev.coilix.processing

import org.slf4j.MDC
import scala.annotation.tailrec
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

/**
 * The work of a shard of a [[dev.coilix.processing.StatelessProcessor]]: polls the source until the shard stops, processes the
 * elements in order and acknowledges each one after `process` returns. Elements polled after a stop was requested are left
 * unacknowledged, so the source delivers them again to the next worker.
 */
private[processing] final class StatelessLoop[T](processor: StatelessProcessor[T]) {

  /** Runs the shard until it stops; throws [[dev.coilix.processing.ElementFailedException]] when an element fails. */
  def run(shard: Shard): Unit = {
    val source = processor.source(shard)
    try {
      checkSlots(shard, source)
      polling(shard, source)
    } finally {
      source.close()
    }
  }

  private def checkSlots(shard: Shard, source: InputSource[T]): Unit =
    source.slotCount.filter(_ != shard.slots.slotCount).foreach { slots =>
      throw InvalidProcessorException(
        s"The source of processor ${shard.processor.name} has $slots slots, but the processor has ${shard.slots.slotCount}; " +
          s"set coilix.processors.${shard.processor.name}.slots = $slots"
      )
    }

  @tailrec
  private def polling(shard: Shard, source: InputSource[T]): Unit =
    if (!shard.stopRequested) {
      val inputs = source.poll(processor.pollSize, processor.pollTimeout)
      shard.metrics.recordLag(source.lag)
      processAll(shard, inputs)
      polling(shard, source)
    }

  @tailrec
  private def processAll(shard: Shard, inputs: List[Input[T]]): Unit =
    inputs match {
      case input :: rest if !shard.stopRequested =>
        processOne(shard, input)
        processAll(shard, rest)
      case _                                     => ()
    }

  private def processOne(shard: Shard, input: Input[T]): Unit = {
    val started = System.nanoTime
    MDC.put("input", input.id.value)
    try {
      processor.process(input.value, ElementContext(shard, input.slot, input.id, input.delivery))
    } catch {
      case NonFatal(error) => failed(shard, input, error)
    } finally {
      MDC.remove("input")
    }
    input.acknowledgement.acknowledge()
    shard.metrics.recordProcessed((System.nanoTime - started).nanos)
  }

  private def failed(shard: Shard, input: Input[T], error: Throwable): Nothing = {
    shard.metrics.recordFailed()
    throw ElementFailedException(shard, input.id, input.delivery, error)
  }
}
