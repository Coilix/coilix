package dev.coilix.concurrency

import dev.coilix.config.Environment

/**
 * Settings of the concurrency module, read from `coilix.concurrency`. Every key is optional.
 *
 * @param compute
 *   settings of the compute pool behind the `taskExecutor` bean
 * @param blocking
 *   settings of the `blockingExecutor` bean
 * @param scheduler
 *   settings of the `taskScheduler` bean
 */
final case class ConcurrencySettings(
  compute:   ComputeSettings   = ComputeSettings(),
  blocking:  BlockingSettings  = BlockingSettings(),
  scheduler: SchedulerSettings = SchedulerSettings()
)

/** Reading of [[dev.coilix.concurrency.ConcurrencySettings]] from the configuration. */
object ConcurrencySettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.concurrency"

  /**
   * The settings under `coilix.concurrency`, with defaults for missing keys.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if a value cannot be read, such as a duration without a unit
   */
  def from(environment: Environment): ConcurrencySettings =
    environment.get[ConcurrencySettings](path).getOrElse(ConcurrencySettings())
}
