package dev.coilix.processing

/**
 * What every background processor declares, whatever its style.
 *
 * A processor class has the `@Processor` annotation and extends one of the styles, such as
 * [[dev.coilix.processing.BlockingProcessor]] or [[dev.coilix.processing.CustomProcessor]]. The number of shards comes
 * from `coilix.processors.<name>.shards`, and [[dev.coilix.processing.ProcessorDefinition.defaultShards]] is used when
 * the configuration has none.
 */
trait ProcessorDefinition {

  /** The identifier of the processor, unique in the application. */
  def id: ProcessorId

  /** The number of shards when the configuration sets none. */
  def defaultShards: Int = 1

  /**
   * Called on the worker thread before every start of the shard's work, including restarts, for example to restore
   * offsets. A failure here is handled like a failure of the work.
   */
  def prepare(shard: Shard): Unit = ()

  /** Called when this replica takes over the shard, before its first start. */
  def onAcquired(shard: Shard): Unit = ()

  /** Called when this replica gives up the shard, after its worker has stopped. */
  def onReleased(shard: Shard, reason: ReleaseReason): Unit = ()
}
