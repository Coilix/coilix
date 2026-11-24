package dev.coilix.processing

import dev.coilix.context.CoilixModule
import dev.coilix.processing.annotation.Processor
import java.lang.annotation.Annotation

/**
 * Adds the [[dev.coilix.processing.ProcessorManager]] when `coilix-processing` is on the classpath and collects the
 * classes with `@Processor` for it instead of making them beans.
 *
 * It can be disabled with `coilix.modules.processing.enabled = false`.
 */
final class ProcessingModule extends CoilixModule {

  override def name: String = "processing"

  override def components: List[Class[?]] = List(classOf[ProcessingConfiguration])

  override def collects: List[Class[? <: Annotation]] = List(classOf[Processor])
}
