package dev.coilix.processing

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.cluster.ReplicaIdentity
import dev.coilix.concurrency.FatalErrorHandler
import dev.coilix.concurrency.TaskScheduler
import dev.coilix.config.Environment
import dev.coilix.context.CollectedComponents
import dev.coilix.events.EventPublisher
import java.time.Clock

/** The beans of the `processing` module. */
@Configuration
final class ProcessingConfiguration {

  /** The settings under `coilix.processors`. */
  @Bean
  def processingSettings(environment: Option[Environment]): ProcessingSettings =
    environment.fold(ProcessingSettings())(ProcessingSettings.from)

  /** The engine that runs blocking and custom processors on virtual threads. */
  @Bean
  def virtualThreadEngine(fatalErrors: FatalErrorHandler): VirtualThreadEngine =
    VirtualThreadEngine(fatalErrors)

  /** The manager of every processor of the application. */
  @Bean
  def processorManager(
    collected:   CollectedComponents,
    engines:     List[ProcessorEngine],
    identity:    ReplicaIdentity,
    settings:    ProcessingSettings,
    scheduler:   TaskScheduler,
    fatalErrors: FatalErrorHandler,
    publisher:   Option[EventPublisher]
  ): DefaultProcessorManager =
    DefaultProcessorManager(collected, engines, identity, settings, scheduler, fatalErrors, publisher, Clock.systemUTC)
}
