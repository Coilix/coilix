package dev.coilix.processing

import dev.coilix.annotation.Slf4s
import dev.coilix.cluster.ReplicaIdentity
import dev.coilix.context.CollectedComponents
import dev.coilix.context.ManagedComponent
import dev.coilix.processing.annotation.Processor
import scala.util.control.NonFatal

/**
 * Creates the processors collected by the context and checks that they can run.
 *
 * Each class with `@Processor` is created once with its dependencies. Two processors with the same identifier, or a
 * processor that no engine or more than one engine runs, end the startup with
 * [[dev.coilix.processing.InvalidProcessorException]], and invalid settings with
 * [[dev.coilix.config.ConfigurationException]]. The processors created before such an error are closed.
 */
@Slf4s
private[processing] final class ProcessorAssembler(
  collected: CollectedComponents,
  engines:   List[ProcessorEngine],
  identity:  ReplicaIdentity,
  settings:  ProcessingSettings
) {

  /** The processors ordered by name. */
  def assemble(): List[ManagedProcessor] = {
    val components = created(collected.annotatedWith[Processor], Nil)
    try {
      val processors = validated(components).map(managed).sortBy(_.id.name)
      warnAboutUnknownNames(processors.map(_.id))
      processors
    } catch {
      case NonFatal(error) =>
        components.foreach(_.close())
        throw error
    }
  }

  private def created(
    classes: List[Class[?]],
    done:    List[ManagedComponent[ProcessorDefinition]]
  ): List[ManagedComponent[ProcessorDefinition]] =
    classes match {
      case Nil          => done.reverse
      case next :: rest => created(rest, createdOrClosed(next, done) :: done)
    }

  private def createdOrClosed(
    processorClass: Class[?],
    done:           List[ManagedComponent[ProcessorDefinition]]
  ): ManagedComponent[ProcessorDefinition] =
    try {
      collected.create[ProcessorDefinition](processorClass)
    } catch {
      case NonFatal(error) =>
        done.foreach(_.close())
        throw error
    }

  private def validated(
    components: List[ManagedComponent[ProcessorDefinition]]
  ): List[ManagedComponent[ProcessorDefinition]] = {
    firstDuplicate(components).foreach(group => throw duplicated(group))
    components
  }

  private def firstDuplicate(
    components: List[ManagedComponent[ProcessorDefinition]]
  ): Option[List[ManagedComponent[ProcessorDefinition]]] =
    components.groupBy(_.instance.id.name).toList.sortBy(_._1).collectFirst { case (_, group) if group.size > 1 => group }

  private def duplicated(duplicates: List[ManagedComponent[ProcessorDefinition]]): InvalidProcessorException =
    InvalidProcessorException(
      s"Processors ${duplicates.map(_.instance.getClass.getName).sorted.mkString(" and ")} have the same identifier " +
        duplicates.head.instance.id.name
    )

  private def managed(component: ManagedComponent[ProcessorDefinition]): ManagedProcessor = {
    val definition = component.instance
    val resolved   = settings.of(definition.id).resolve(definition.id.name, definition.defaultShards, identity.replicaCount)
    val engine     = engineOf(definition)
    ManagedProcessor(
      ProcessorDescriptor(definition.id, definition.getClass, engine.name, resolved),
      component,
      engine,
      resolved.assignment
        .shardsOf(identity.self.index, resolved.shardCount, identity.replicaCount)
        .map(SlotSet(_, resolved.shardCount, resolved.slotCount))
    )
  }

  private def engineOf(definition: ProcessorDefinition): ProcessorEngine =
    engines.filter(_.supports(definition)) match {
      case engine :: Nil => engine
      case Nil           => throw InvalidProcessorException(s"${describe(definition)} has no engine: $noEngineHint")
      case several       =>
        throw InvalidProcessorException(s"${describe(definition)} has several engines: ${several.map(_.name).mkString(", ")}")
    }

  private def describe(definition: ProcessorDefinition): String =
    s"Processor ${definition.id.name} (${definition.getClass.getName})"

  private val noEngineHint: String =
    "extend IOProcessor, or add the module that runs its style of processors"

  private def warnAboutUnknownNames(ids: List[ProcessorId]): Unit =
    settings.unknown(ids).foreach { name =>
      log.warn(
        s"Configuration ${ProcessingSettings.path}.$name belongs to no processor; known processors: ${ids.map(_.name).mkString(", ")}"
      )
    }
}
