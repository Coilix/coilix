package dev.coilix.processing

/** A shard of a processor, as a key of the state of the manager. */
private[processing] final case class ShardKey(id: ProcessorId, shard: Int) {

  override def toString: String = s"${id.name}#$shard"
}
