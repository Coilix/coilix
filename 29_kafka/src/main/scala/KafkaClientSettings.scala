package dev.coilix.kafka

import org.apache.kafka.clients.CommonClientConfigs

/**
 * The settings of one Kafka client, that is of one cluster, under `coilix.kafka.clients.<name>`.
 *
 * @param bootstrapServers
 *   the brokers to connect to first, separated by commas
 * @param clientId
 *   the `client.id` sent to the brokers; without it the name of the client
 * @param auth
 *   the security protocol and the credentials
 * @param consumer
 *   the settings of the consumers
 * @param producer
 *   the settings of the producer
 * @param commit
 *   how consumed offsets are committed in batches
 * @param admin
 *   the settings of the admin client and of the topics it creates
 */
case class KafkaClientSettings(
  bootstrapServers: String,
  clientId:         Option[String]        = None,
  auth:             KafkaAuthSettings     = KafkaAuthSettings(),
  consumer:         KafkaConsumerSettings = KafkaConsumerSettings(),
  producer:         KafkaProducerSettings = KafkaProducerSettings(),
  commit:           KafkaCommitSettings   = KafkaCommitSettings(),
  admin:            KafkaAdminSettings    = KafkaAdminSettings()
) {

  /** The properties shared by the consumers, the producer and the admin client of the client named `name`. */
  def commonProperties(name: String): Map[String, String] =
    Map(
      CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers,
      CommonClientConfigs.CLIENT_ID_CONFIG         -> clientId.getOrElse(name)
    ) ++ auth.properties
}
