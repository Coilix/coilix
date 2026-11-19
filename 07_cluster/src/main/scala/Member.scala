package dev.coilix.cluster

import java.time.Instant

/**
 * One replica of the application.
 *
 * @param index
 *   the number of the replica, from `0` to the number of replicas minus one
 * @param id
 *   a unique name of the replica, such as the pod name
 * @param address
 *   where other replicas reach it, or `None` when replicas do not talk to each other
 * @param startedAt
 *   when the replica started
 * @param version
 *   the version of the application that the replica runs
 */
final case class Member(index: Int, id: String, address: Option[MemberAddress], startedAt: Instant, version: String)
