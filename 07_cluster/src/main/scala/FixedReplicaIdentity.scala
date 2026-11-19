package dev.coilix.cluster

/**
 * A [[dev.coilix.cluster.ReplicaIdentity]] that does not change while the application runs, as resolved at startup by the
 * [[dev.coilix.cluster.ReplicaIdentityResolver]].
 *
 * @param self
 *   this replica
 * @param replicaCount
 *   the number of replicas
 */
final case class FixedReplicaIdentity(self: Member, replicaCount: Int) extends ReplicaIdentity
