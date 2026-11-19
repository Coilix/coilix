package dev.coilix.cluster

/**
 * Which replica of the application this is, and how many replicas there are.
 *
 * The cluster module registers a `replicaIdentity` bean resolved from `coilix.cluster`. Modules use it to split work between
 * replicas without talking to each other:
 *
 * ```scala
 * @Component
 * final class ShardAssignment(identity: ReplicaIdentity) {
 *   def owns(shard: Int): Boolean = shard % identity.replicaCount == identity.self.index
 * }
 * ```
 */
trait ReplicaIdentity {

  /** This replica. */
  def self: Member

  /** The number of replicas, at least `1`. */
  def replicaCount: Int
}
