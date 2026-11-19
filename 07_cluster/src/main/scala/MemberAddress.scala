package dev.coilix.cluster

/**
 * The network address at which other replicas reach a replica.
 *
 * @param host
 *   a host name or IP address
 * @param port
 *   the cluster port, separate from the public API port
 */
final case class MemberAddress(host: String, port: Int)
