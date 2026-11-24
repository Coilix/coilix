package dev.coilix.processing

/**
 * The result of one event in [[dev.coilix.processing.ShardSupervision]].
 *
 * @param supervision
 *   the supervision after the event
 * @param commands
 *   what the manager does, in order
 */
final case class SupervisionStep(supervision: ShardSupervision, commands: List[WorkerCommand])
