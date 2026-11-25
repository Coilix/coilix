package dev.coilix.processing
package misfit

import dev.coilix.processing.annotation.Processor

/** Reads a source with 16 slots, while the processor keeps the default 256. */
@Processor
final class Misfit extends StatelessProcessor[String] {
  private val stream = InMemoryStream[String](16, identity)

  override val id: TestProcessor = TestProcessor.Misfit

  override def source(shard: Shard): InputSource[String] = stream.source(shard)

  override def process(element: String, context: ElementContext): Unit = ()
}
