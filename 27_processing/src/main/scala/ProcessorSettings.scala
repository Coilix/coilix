package dev.coilix.processing

import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * The keys of one processor under `coilix.processors.<name>`, or of `coilix.processors.defaults`, each optional.
 *
 * `shards` is read only for a processor, where it takes precedence over
 * [[dev.coilix.processing.ProcessorDefinition.defaultShards]].
 *
 * @param shards
 *   the number of shards
 * @param slots
 *   the number of slots, 256 by default
 * @param assignment
 *   `round-robin` (the default), `range`, `all` or `pinned`
 * @param pinnedReplica
 *   the replica of `assignment = pinned`, 0 by default
 * @param enabled
 *   `false` registers the processor as `Stopped`, so it starts only when started through the manager
 * @param onCompletion
 *   `stop` (the default) or `restart`
 * @param completionDelay
 *   the delay of `on-completion = restart`, 1 second by default
 * @param drainTimeout
 *   how long the work of a shard may run after the stop signal, for example to finish the elements a
 *   [[dev.coilix.processing.StreamProcessor]] has already read, before it is cancelled; half of the stop timeout by default
 * @param restart
 *   the restart policy
 */
final case class ProcessorSettings(
  shards:          Option[Int]            = None,
  slots:           Option[Int]            = None,
  assignment:      Option[String]         = None,
  pinnedReplica:   Option[Int]            = None,
  enabled:         Option[Boolean]        = None,
  onCompletion:    Option[String]         = None,
  completionDelay: Option[FiniteDuration] = None,
  drainTimeout:    Option[FiniteDuration] = None,
  restart:         RestartSettings        = RestartSettings()
) {

  /** These settings with each missing key taken from `fallback`. */
  def orElse(fallback: ProcessorSettings): ProcessorSettings =
    ProcessorSettings(
      shards.orElse(fallback.shards),
      slots.orElse(fallback.slots),
      assignment.orElse(fallback.assignment),
      pinnedReplica.orElse(fallback.pinnedReplica),
      enabled.orElse(fallback.enabled),
      onCompletion.orElse(fallback.onCompletion),
      completionDelay.orElse(fallback.completionDelay),
      drainTimeout.orElse(fallback.drainTimeout),
      restart.orElse(fallback.restart)
    )

  /**
   * The settings of the processor with the defaults of Coilix for the missing keys.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   when a value is out of range or unknown
   */
  def resolve(name: String, defaultShards: Int, replicaCount: Int): ResolvedProcessorSettings = {
    val shardCount = shards.getOrElse(defaultShards)
    val slotCount  = slots.getOrElse(ProcessorSettings.defaultSlots)
    validate(name, shardCount, slotCount, replicaCount)
    ResolvedProcessorSettings(
      shardCount,
      slotCount,
      ShardAssignment.named(assignment.getOrElse(ShardAssignment.roundRobin.name), pinnedReplica.getOrElse(0)),
      enabled.getOrElse(true),
      SupervisionPolicy(restart.policy, completion(name)),
      drainTimeout
    )
  }

  private def validate(name: String, shardCount: Int, slotCount: Int, replicaCount: Int): Unit = {
    if (shardCount < 1) throw invalid(name, s"has $shardCount shards, at least 1 is needed")
    if (slotCount < shardCount) throw invalid(name, s"has $shardCount shards but only $slotCount slots")
    drainTimeout.filter(_ <= Duration.Zero).foreach(timeout =>
      throw invalid(name, s"has drain-timeout $timeout, it must be positive")
    )
    pinnedReplica.filter(replica => replica < 0 || replica >= replicaCount).foreach { replica =>
      throw invalid(name, s"is pinned to replica $replica, but the application has replicas 0 to ${replicaCount - 1}")
    }
  }

  private def completion(name: String): CompletionPolicy =
    onCompletion.getOrElse("stop") match {
      case "stop"    => CompletionPolicy.Stop
      case "restart" => CompletionPolicy.Restart(completionDelay.getOrElse(1.second))
      case other     => throw invalid(name, s"has unknown on-completion '$other', expected stop or restart")
    }

  private def invalid(name: String, problem: String): ConfigurationException =
    ConfigurationException(s"Processor $name $problem")
}

/** Reads processor settings. */
object ProcessorSettings {

  /** The number of slots of a processor whose configuration sets none. */
  val defaultSlots: Int = 256

  /** Reads the keys under `path`, with `shards` only when `withShards` is true. */
  def read(environment: Environment, path: String, withShards: Boolean): ProcessorSettings =
    ProcessorSettings(
      shards          = Option.when(withShards)(environment.get[Int](s"$path.shards")).flatten,
      slots           = environment.get[Int](s"$path.slots"),
      assignment      = environment.get[String](s"$path.assignment"),
      pinnedReplica   = environment.get[Int](s"$path.pinned-replica"),
      enabled         = environment.get[Boolean](s"$path.enabled"),
      onCompletion    = environment.get[String](s"$path.on-completion"),
      completionDelay = environment.get[FiniteDuration](s"$path.completion-delay"),
      drainTimeout    = environment.get[FiniteDuration](s"$path.drain-timeout"),
      restart         = RestartSettings.read(environment, s"$path.restart")
    )
}
