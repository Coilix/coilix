package dev.coilix.cluster

import java.time.Clock
import scala.util.Try

/**
 * Resolves the [[dev.coilix.cluster.ReplicaIdentity]] of this replica from the settings and the environment variables.
 *
 * The `static` mode falls back to the `COILIX_REPLICAS` and `COILIX_REPLICA_INDEX` variables, and the `stateful-set` mode to
 * `COILIX_REPLICAS`, `COILIX_POD_INDEX` and the number at the end of the pod name. The name of the replica is
 * `coilix.cluster.replica-id`, the `HOSTNAME` variable or `replica-<index>`.
 *
 * @param variables
 *   the environment variables, `sys.env` in an application
 * @param clock
 *   gives the start time of the replica
 */
final class ReplicaIdentityResolver(variables: Map[String, String], clock: Clock) {

  /**
   * The identity described by `settings`.
   *
   * @throws dev.coilix.cluster.ReplicaIdentityException
   *   if a required number is missing or not a number, the count is below `1`, or the index is outside `0` until the count
   */
  def resolve(settings: ClusterSettings): ReplicaIdentity =
    settings.identity match {
      case IdentityMode.Single      => identity(settings, 0, 1)
      case IdentityMode.Static      => identity(settings, staticIndex(settings), replicas(settings))
      case IdentityMode.StatefulSet => identity(settings, podIndex(settings), replicas(settings))
    }

  private def identity(settings: ClusterSettings, index: Int, count: Int): ReplicaIdentity =
    if (count < 1) throw ReplicaIdentityException(s"coilix.cluster.replicas must be at least 1, but is $count")
    else if (index < 0 || index >= count) throw outOfRange(index, count)
    else FixedReplicaIdentity(member(settings, index), count)

  private def member(settings: ClusterSettings, index: Int): Member =
    Member(index, replicaId(settings).getOrElse(s"replica-$index"), None, clock.instant, settings.version)

  private def replicas(settings: ClusterSettings): Int =
    settings.replicas
      .orElse(number("COILIX_REPLICAS"))
      .getOrElse(throw missing("coilix.cluster.replicas", "COILIX_REPLICAS", "set it to the replicas of the deployment"))

  private def staticIndex(settings: ClusterSettings): Int =
    settings.replicaIndex
      .orElse(number("COILIX_REPLICA_INDEX"))
      .getOrElse(throw missing("coilix.cluster.replica-index", "COILIX_REPLICA_INDEX", "give each replica its own number"))

  private def podIndex(settings: ClusterSettings): Int =
    settings.replicaIndex.orElse(number("COILIX_POD_INDEX")).orElse(replicaId(settings).flatMap(nameSuffix)).getOrElse {
      throw ReplicaIdentityException(
        "Cannot find the number of this StatefulSet pod. Pass the apps.kubernetes.io/pod-index label as COILIX_POD_INDEX " +
          "through the downward API, or make sure the pod name ends with its number, such as shop-3"
      )
    }

  private def replicaId(settings: ClusterSettings): Option[String] =
    settings.replicaId.orElse(variables.get("HOSTNAME")).filter(_.nonEmpty)

  private def nameSuffix(name: String): Option[Int] =
    name.split('-').lastOption.flatMap(_.toIntOption).filter(_ => name.contains('-'))

  private def number(variable: String): Option[Int] =
    variables.get(variable).map { value =>
      Try(value.trim.toInt).getOrElse(throw ReplicaIdentityException(s"$variable must be a number, but is '$value'"))
    }

  private def missing(key: String, variable: String, hint: String): ReplicaIdentityException =
    ReplicaIdentityException(s"Missing $key, also not given as $variable: $hint")

  private def outOfRange(index: Int, count: Int): ReplicaIdentityException =
    ReplicaIdentityException(s"Replica index $index is outside 0 until $count. Check coilix.cluster.replicas and the replica index")
}
