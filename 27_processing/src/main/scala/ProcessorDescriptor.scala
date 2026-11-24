package dev.coilix.processing

/**
 * What the manager knows about a processor that does not change while the application runs.
 *
 * @param id
 *   the identifier of the processor
 * @param processorClass
 *   the class with the `@Processor` annotation
 * @param engine
 *   the name of the [[dev.coilix.processing.ProcessorEngine]] that runs it
 * @param settings
 *   the effective settings
 */
final case class ProcessorDescriptor(id: ProcessorId, processorClass: Class[?], engine: String, settings: ResolvedProcessorSettings)
