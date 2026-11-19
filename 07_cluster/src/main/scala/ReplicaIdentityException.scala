package dev.coilix.cluster

import dev.coilix.common.CoilixException

/** Thrown at startup when the replica cannot find out its number, with a hint about the missing or wrong setting. */
final class ReplicaIdentityException(message: String) extends CoilixException(message)
