package dev.coilix.cluster

import dev.coilix.context.CoilixModule

/**
 * Adds the [[dev.coilix.cluster.ReplicaIdentity]] bean when `coilix-cluster` is on the classpath.
 *
 * Modules that split work between replicas depend on `coilix-cluster`, so an application gets it with them. It can be
 * disabled with `coilix.modules.cluster.enabled = false`.
 */
final class ClusterModule extends CoilixModule {

  override def name: String = "cluster"

  override def components: List[Class[?]] = List(classOf[ClusterConfiguration])
}
