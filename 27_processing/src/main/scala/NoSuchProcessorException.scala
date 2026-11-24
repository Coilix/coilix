package dev.coilix.processing

import dev.coilix.common.CoilixException

/** Thrown by the [[dev.coilix.processing.ProcessorManager]] for an identifier that has no processor. */
final class NoSuchProcessorException(id: ProcessorId)
    extends CoilixException(s"No processor ${id.name}: no class with @Processor has this identifier")
