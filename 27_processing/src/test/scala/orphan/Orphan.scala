package dev.coilix.processing
package orphan

import dev.coilix.processing.annotation.Processor

/** A processor of a style that no engine runs. */
@Processor
final class Orphan extends ProcessorDefinition {

  override val id: TestProcessor = TestProcessor.Orphan
}
