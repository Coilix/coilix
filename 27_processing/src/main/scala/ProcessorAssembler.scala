package dev.coilix.processing

import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.cluster.ReplicaIdentity
import dev.coilix.context.CollectedComponents
import dev.coilix.context.ManagedComponent
import dev.coilix.processing.annotation.Processor
import scala.util.control.NonFatal

/**
 * Creates the processors collected by the context and checks that they can run.
 *
 * Each class with `@Processor` is created once with its dependencies. Two processors with the same identifier, a processor
 * that no engine or more than one engine runs, and invalid settings are returned as a
 * [[dev.coilix.processing.ProcessingSetupError]]. The processors created before such an error, or before a failure to
 * create a processor, are closed.
 */
@Slf4s
private[processing] final class ProcessorAssembler(
  collected: CollectedComponents,
  engines:   List[ProcessorEngine],
  identity:  ReplicaIdentity,
  settings:  ProcessingSettings
) {

  /** The processors ordered by name, or the reason why they cannot run. */
  def assemble(): Either[ProcessingSetupError, List[ManagedProcessor]] = {
    val components = created(collected.annotatedWith[Processor], Nil)
    val assembled  =
      try {
        validated(components).flatMap(_.traverse(managed)).map(_.sortBy(_.id.name))
      } catch {
        case NonFatal(error) =>
          components.foreach(_.close())
          throw error
      }
    assembled.fold(error => closedAfter(components, error), processors => Right(warnedAbout(processors)))
  }

  private def closedAfter(
    components: List[ManagedComponent[ProcessorDefinition]],
    error:      ProcessingSetupError
  ): Either[ProcessingSetupError, List[ManagedProcessor]] = {
    components.foreach(_.close())
    Left(error)
  }

  private def warnedAbout(processors: List[ManagedProcessor]): List[ManagedProcessor] = {
    warnAboutUnknownNames(processors.map(_.id))
    processors
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
  ): Either[ProcessingSetupError, List[ManagedComponent[ProcessorDefinition]]] =
    firstDuplicate(components).map(duplicated).toLeft(components)

  private def firstDuplicate(
    components: List[ManagedComponent[ProcessorDefinition]]
  ): Option[List[ManagedComponent[ProcessorDefinition]]] =
    components.groupBy(_.instance.id.name).toList.sortBy(_._1).collectFirst { case (_, group) if group.size > 1 => group }

  private def duplicated(duplicates: List[ManagedComponent[ProcessorDefinition]]): ProcessingSetupError =
    ProcessingSetupError.DuplicateIdentifier(
      duplicates.head.instance.id.name,
      duplicates.map(_.instance.getClass.getName).sorted
    )

  private def managed(component: ManagedComponent[ProcessorDefinition]): Either[ProcessingSetupError, ManagedProcessor] = {
    val definition = component.instance
    for {
      resolved <- settings.of(definition.id).resolve(definition.id.name, definition.defaultShards, identity.replicaCount)
      engine   <- engineOf(definition)
    } yield managedWith(component, resolved, engine)
  }

  private def managedWith(
    component: ManagedComponent[ProcessorDefinition],
    resolved:  ResolvedProcessorSettings,
    engine:    ProcessorEngine
  ): ManagedProcessor = {
    val definition = component.instance
    ManagedProcessor(
      ProcessorDescriptor(definition.id, definition.getClass, engine.name, resolved),
      component,
      engine,
      resolved.assignment
        .shardsOf(identity.self.index, resolved.shardCount, identity.replicaCount)
        .map(SlotSet(_, resolved.shardCount, resolved.slotCount))
    )
  }

  private def engineOf(definition: ProcessorDefinition): Either[ProcessingSetupError, ProcessorEngine] =
    engines.filter(_.supports(definition)) match {
      case engine :: Nil => Right(engine)
      case Nil           => Left(ProcessingSetupError.NoEngine(definition.id.name, definition.getClass))
      case several       => Left(ProcessingSetupError.SeveralEngines(definition.id.name, definition.getClass, several.map(_.name)))
    }

  private def warnAboutUnknownNames(ids: List[ProcessorId]): Unit =
    settings.unknown(ids).foreach { name =>
      log.warn(
        s"Configuration ${ProcessingSettings.path}.$name belongs to no processor; known processors: ${ids.map(_.name).mkString(", ")}"
      )
    }
}
