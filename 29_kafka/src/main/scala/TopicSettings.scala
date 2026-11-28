package dev.coilix.kafka

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * How [[dev.coilix.kafka.KafkaAdmin.createTopics]] creates a topic. The replication factor and `min.insync.replicas` come
 * from the [[dev.coilix.kafka.KafkaAdminSettings]] of the client, because they depend on the cluster.
 *
 * @param partitions
 *   the number of partitions, which also limits how many consumers of a group read at the same time
 * @param cleanupPolicy
 *   what Kafka does with old records
 * @param retention
 *   how long records are kept; `None` keeps them forever
 * @param retentionBytes
 *   the most bytes a partition keeps
 * @param maxMessageBytes
 *   the largest record batch the topic accepts
 * @param properties
 *   other topic properties by their Kafka names, such as `segment.bytes`
 */
case class TopicSettings(
  partitions:      Int                    = 12,
  cleanupPolicy:   CleanupPolicy          = CleanupPolicy.Delete,
  retention:       Option[FiniteDuration] = Some(7.days),
  retentionBytes:  Option[Long]           = None,
  maxMessageBytes: Option[Int]            = None,
  properties:      Map[String, String]    = Map.empty
) {

  /** The Kafka properties of the topic with the given `min.insync.replicas`. */
  def kafkaProperties(minInSyncReplicas: Int): Map[String, String] =
    Map(
      "cleanup.policy"      -> cleanupPolicy.kafkaName,
      "retention.ms"        -> retention.fold("-1")(_.toMillis.toString),
      "min.insync.replicas" -> minInSyncReplicas.toString
    ) ++ List(
      retentionBytes.map("retention.bytes" -> _.toString),
      maxMessageBytes.map("max.message.bytes" -> _.toString)
    ).flatten ++ properties
}
