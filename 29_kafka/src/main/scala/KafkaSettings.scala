package dev.coilix.kafka

import dev.coilix.config.Environment

/**
 * The Kafka clients of the application under `coilix.kafka.clients`, one per cluster, by their names.
 *
 * ```yaml
 * coilix.kafka.clients:
 *   default:
 *     bootstrap-servers: kafka-1:9092,kafka-2:9092
 *   telemetry:
 *     bootstrap-servers: telemetry.example.com:9093
 *     auth:
 *       security-protocol: sasl-ssl
 *       sasl-mechanism: SCRAM-SHA-512
 *       sasl-jaas-config: ${TELEMETRY_KAFKA_JAAS}
 *     consumer:
 *       max-poll-records: 200
 *     commit:
 *       max-batch: 1000
 * ```
 *
 * Without the section the application has no clients. A topic names its client with [[dev.coilix.kafka.KafkaTopic.client]], by default [[dev.coilix.kafka.KafkaClients.Default]].
 *
 * @param clients
 *   the settings of each client by its name
 */
case class KafkaSettings(clients: Map[String, KafkaClientSettings] = Map.empty)

/** Reads the Kafka clients from the configuration. */
object KafkaSettings {

  /** The section of the settings. */
  val path: String = "coilix.kafka"

  /** The clients under `coilix.kafka`, or none when the section is missing. */
  def from(environment: Environment): KafkaSettings =
    environment.get[KafkaSettings](path).getOrElse(KafkaSettings())
}
