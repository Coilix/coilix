package dev.coilix.processing

import dev.coilix.context.ManagedComponent

/**
 * A processor created by the manager, with the engine that runs it and the shards of this replica.
 *
 * @param descriptor
 *   what the manager tells about the processor
 * @param component
 *   the instance, closed with its `@PreDestroy` methods when the manager closes
 * @param engine
 *   the engine that runs its shards
 * @param shards
 *   the shards of this replica
 */
private[processing] final case class ManagedProcessor(
  descriptor: ProcessorDescriptor,
  component:  ManagedComponent[ProcessorDefinition],
  engine:     ProcessorEngine,
  shards:     List[SlotSet]
) {

  def id: ProcessorId = descriptor.id

  def definition: ProcessorDefinition = component.instance

  def settings: ResolvedProcessorSettings = descriptor.settings
}
