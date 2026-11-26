package dev.coilix.processing

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.cluster.ReplicaIdentity
import dev.coilix.config.Environment
import dev.coilix.context.CollectedComponents

/**
 * The beans of the `processing` module: `processingSettings`, `fiberEngine`, which runs
 * [[dev.coilix.processing.IOProcessor]] shards on fibers, and `processorManager`.
 */
@Configuration
final class ProcessingConfiguration {

  /** The settings under `coilix.processors`, or the defaults in a context without an environment. */
  @Bean
  def processingSettings(environment: Option[Environment]): ProcessingSettings =
    environment.fold(ProcessingSettings())(ProcessingSettings.from)

  /** The engine of [[dev.coilix.processing.IOProcessor]], whose fibers are cancelled when the context closes. */
  @Bean
  def fiberEngine(): Resource[IO, FiberEngine] = FiberEngine.resource

  /** The manager of every processor, which closes the processors when the context closes. */
  @Bean
  def processorManager(
    collected: CollectedComponents,
    engines:   List[ProcessorEngine],
    identity:  ReplicaIdentity,
    settings:  ProcessingSettings
  ): Resource[IO, DefaultProcessorManager] =
    DefaultProcessorManager.resource(collected, engines, identity, settings)
}
