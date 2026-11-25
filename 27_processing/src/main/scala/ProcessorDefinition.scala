package dev.coilix.processing

import cats.effect.IO

/**
 * What every background processor declares, whatever its style.
 *
 * A processor class has the `@Processor` annotation and extends one of the styles, such as
 * [[dev.coilix.processing.IOProcessor]]. The number of shards comes from `coilix.processors.<name>.shards`, and
 * [[dev.coilix.processing.ProcessorDefinition.defaultShards]] is used when the configuration has none.
 */
trait ProcessorDefinition {

  /** The identifier of the processor, unique in the application. */
  def id: ProcessorId

  /** The number of shards when the configuration sets none. */
  def defaultShards: Int = 1

  /**
   * Runs before every start of the shard's work, including restarts, for example to restore offsets. A failure here is
   * handled like a failure of the work.
   */
  def prepare(shard: Shard): IO[Unit] = IO.unit

  /** Runs when this replica takes over the shard, before its first start. */
  def onAcquired(shard: Shard): IO[Unit] = IO.unit

  /** Runs when this replica gives up the shard, after its worker has stopped. */
  def onReleased(shard: Shard, reason: ReleaseReason): IO[Unit] = IO.unit
}
