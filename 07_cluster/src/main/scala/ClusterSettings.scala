package dev.coilix.cluster

import dev.coilix.config.Environment

/**
 * Settings of the cluster module, read from `coilix.cluster`. Every key is optional.
 *
 * ```yaml
 * coilix.cluster:
 *   identity: stateful-set
 *   replicas: 3
 * ```
 *
 * @param identity
 *   how the replica finds out its number
 * @param replicas
 *   the number of replicas, required by the `static` and `stateful-set` modes
 * @param replicaIndex
 *   the number of this replica, required by the `static` mode
 * @param replicaId
 *   the name of this replica, by default the `HOSTNAME` variable, which is the pod name in Kubernetes
 * @param version
 *   the version of the application, shown to other replicas
 */
final case class ClusterSettings(
  identity:     IdentityMode   = IdentityMode.Single,
  replicas:     Option[Int]    = None,
  replicaIndex: Option[Int]    = None,
  replicaId:    Option[String] = None,
  version:      String         = "unknown"
)

/** Reading of [[dev.coilix.cluster.ClusterSettings]] from the configuration. */
object ClusterSettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.cluster"

  /**
   * The settings under `coilix.cluster`, with defaults for missing keys.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if a value cannot be read, such as an unknown identity mode
   */
  def from(environment: Environment): ClusterSettings =
    ClusterSettings(
      identity     = environment.get[IdentityMode](s"$path.identity").getOrElse(IdentityMode.Single),
      replicas     = environment.get[Int](s"$path.replicas"),
      replicaIndex = environment.get[Int](s"$path.replica-index"),
      replicaId    = environment.get[String](s"$path.replica-id"),
      version      = environment.get[String](s"$path.version").getOrElse("unknown")
    )
}
