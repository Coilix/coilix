package dev.coilix.processing

import dev.coilix.common.CoilixException

/**
 * Thrown at startup when the processors cannot run: two processors with the same identifier, or a processor that no
 * [[dev.coilix.processing.ProcessorEngine]] or more than one runs.
 */
final class InvalidProcessorException(message: String) extends CoilixException(message)
