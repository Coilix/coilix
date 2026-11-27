package dev.coilix.processing

import dev.coilix.common.CoilixException
import dev.coilix.config.ConfigurationException

/**
 * A predictable reason why the processors of the application cannot be set up, returned by
 * [[dev.coilix.processing.ProcessorSettings.resolve]] and [[dev.coilix.processing.ShardAssignment.named]].
 *
 * The `processorManager` bean is the top layer of the setup: it fails its creation with the
 * [[dev.coilix.processing.ProcessingSetupError.exception]] of the error, which stops the startup.
 */
enum ProcessingSetupError {

  /**
   * A setting of a processor is out of range or unknown.
   *
   * @param processor
   *   the name of the processor
   * @param problem
   *   what is wrong, such as `has 0 shards, at least 1 is needed`
   */
  case InvalidSettings(processor: String, problem: String)

  /**
   * The configured shard assignment is none of `round-robin`, `range`, `all` and `pinned`.
   *
   * @param name
   *   the configured name
   */
  case UnknownAssignment(name: String)

  /**
   * Several classes with `@Processor` have the same identifier.
   *
   * @param id
   *   the shared identifier
   * @param classes
   *   the names of the classes, sorted
   */
  case DuplicateIdentifier(id: String, classes: List[String])

  /**
   * No engine runs the processor.
   *
   * @param processor
   *   the name of the processor
   * @param processorClass
   *   the class of the processor
   */
  case NoEngine(processor: String, processorClass: Class[?])

  /**
   * More than one engine runs the processor.
   *
   * @param processor
   *   the name of the processor
   * @param processorClass
   *   the class of the processor
   * @param engines
   *   the names of the engines
   */
  case SeveralEngines(processor: String, processorClass: Class[?], engines: List[String])

  /** The description of this error. */
  def message: String =
    this match {
      case InvalidSettings(processor, problem)                => s"Processor $processor $problem"
      case UnknownAssignment(name)                            => unknownAssignment(name)
      case DuplicateIdentifier(id, classes)                   => s"Processors ${classes.mkString(" and ")} have the same identifier $id"
      case NoEngine(processor, processorClass)                => s"${describe(processor, processorClass)} has no engine: $noEngineHint"
      case SeveralEngines(processor, processorClass, engines) => severalEngines(processor, processorClass, engines)
    }

  /**
   * The exception that reports this error to code that throws: a [[dev.coilix.config.ConfigurationException]] for settings
   * and an [[dev.coilix.processing.InvalidProcessorException]] for processors.
   */
  def exception: CoilixException =
    this match {
      case InvalidSettings(_, _) | UnknownAssignment(_) => ConfigurationException(message)
      case _                                            => InvalidProcessorException(message)
    }

  private def unknownAssignment(name: String): String =
    s"Unknown shard assignment '$name', expected round-robin, range, all or pinned"

  private def severalEngines(processor: String, processorClass: Class[?], engines: List[String]): String =
    s"${describe(processor, processorClass)} has several engines: ${engines.mkString(", ")}"

  private def describe(processor: String, processorClass: Class[?]): String =
    s"Processor $processor (${processorClass.getName})"

  private def noEngineHint: String =
    "extend IOProcessor, or add the module that runs its style of processors"
}
