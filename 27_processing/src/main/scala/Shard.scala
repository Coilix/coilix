package dev.coilix.processing

import cats.effect.Deferred
import cats.effect.IO
import dev.coilix.cluster.Member
import scala.concurrent.duration.FiniteDuration

/**
 * One shard of a processor that runs on this replica: its index, its slots, the replica that runs it and the signal that
 * stops it.
 *
 * The manager gives a new `Shard` to every start of the shard's worker. A processor checks
 * [[dev.coilix.processing.Shard.stopRequested]] between steps, or waits on [[dev.coilix.processing.Shard.stopped]], to end
 * its work when the shard stops:
 *
 * ```scala
 * override def run(shard: Shard): IO[Unit] =
 *   shard.stopRequested.ifM(IO.unit, exportNext(shard.slots) *> run(shard))
 * ```
 *
 * @param processor
 *   the identifier of the processor
 * @param slots
 *   the index of the shard, the number of shards and the slots of the shard
 * @param replica
 *   the replica that runs the shard
 * @param stopSignal
 *   completed when the shard is asked to stop
 * @param drainTimeout
 *   how long the work may run after the stop signal before it is cancelled, from `drain-timeout` of the processor; without it
 *   half of the stop timeout
 */
case class Shard(
  processor:    ProcessorId,
  slots:        SlotSet,
  replica:      Member,
  stopSignal:   Deferred[IO, Unit],
  drainTimeout: Option[FiniteDuration] = None
) {

  /** The index of the shard, from 0 until [[dev.coilix.processing.Shard.count]]. */
  def index: Int = slots.shard

  /** The number of shards of the processor. */
  def count: Int = slots.shardCount

  /** Whether the slot belongs to this shard. */
  def owns(slot: Int): Boolean = slots.contains(slot)

  /** Whether the shard is stopping, so the processor should finish its current step and end. */
  def stopRequested: IO[Boolean] = stopSignal.tryGet.map(_.isDefined)

  /** Completes when the shard is asked to stop. */
  def stopped: IO[Unit] = stopSignal.get

  /** Asks the processor to stop its work on this shard. Asking again does nothing. */
  def requestStop: IO[Unit] = stopSignal.complete(()).void

  /** The name of the processor with the index of the shard, such as `invoice-export#2`. */
  override def toString: String = s"${processor.name}#$index"
}

/** Creation of [[dev.coilix.processing.Shard]] values. */
object Shard {

  /** A shard that is not stopping yet, with the `drain-timeout` of its processor. */
  def create(
    processor:    ProcessorId,
    slots:        SlotSet,
    replica:      Member,
    drainTimeout: Option[FiniteDuration] = None
  ): IO[Shard] =
    Deferred[IO, Unit].map(signal => Shard(processor, slots, replica, signal, drainTimeout))
}
