package dev.coilix.kafka

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * The settings of the admin client of a Kafka client and of the topics it creates.
 *
 * @param replicationFactor
 *   the number of replicas of a created topic
 * @param minInSyncReplicas
 *   the `min.insync.replicas` of a created topic
 * @param timeout
 *   how long one admin operation may take
 * @param warnIfSlowerThan
 *   an operation that takes longer is logged as a warning
 * @param properties
 *   other Kafka admin properties by their Kafka names
 */
case class KafkaAdminSettings(
  replicationFactor: Int                 = 1,
  minInSyncReplicas: Int                 = 1,
  timeout:           FiniteDuration      = 10.seconds,
  warnIfSlowerThan:  FiniteDuration      = 750.millis,
  properties:        Map[String, String] = Map.empty
) {

  /** The Kafka properties of these settings. */
  def kafkaProperties: Map[String, String] =
    Map(
      "default.api.timeout.ms" -> timeout.toMillis.toString,
      "request.timeout.ms"     -> timeout.toMillis.toString
    ) ++ properties
}
