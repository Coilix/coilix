package dev.coilix.cluster

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Slf4s
import dev.coilix.config.Environment
import java.time.Clock

/** The beans of the cluster module: `clusterSettings` and `replicaIdentity`. */
@Slf4s
@Configuration
final class ClusterConfiguration {

  /** The settings under `coilix.cluster`, or the defaults in a context without an environment. */
  @Bean
  def clusterSettings(environment: Option[Environment]): ClusterSettings =
    environment.fold(ClusterSettings())(ClusterSettings.from)

  /**
   * The identity of this replica, resolved from the settings and the environment variables.
   *
   * @throws dev.coilix.cluster.ReplicaIdentityException
   *   if the identity cannot be resolved, which stops the startup
   */
  @Bean
  def replicaIdentity(settings: ClusterSettings): ReplicaIdentity = {
    val identity = ReplicaIdentityResolver(sys.env, Clock.systemUTC).resolve(settings)
    log.info(s"Running as replica ${identity.self.index} of ${identity.replicaCount} (${identity.self.id})")
    identity
  }
}
