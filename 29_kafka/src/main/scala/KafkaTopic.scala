package dev.coilix.kafka

/**
 * A Kafka topic with the types of its keys and values, the client of the cluster it lives on and how it is created.
 *
 * Topics are usually values in an object of the application, so producers and consumers share them:
 *
 * ```scala
 * object ShopTopics {
 *   val orders: KafkaTopic[String, String] = KafkaTopic("shop.orders")
 *   val readings: KafkaTopic[String, Long] =
 *     KafkaTopic("plant.readings", client = "telemetry", settings = TopicSettings(partitions = 24))
 * }
 * ```
 *
 * @param name
 *   the name of the topic
 * @param keys
 *   the codec of the keys
 * @param values
 *   the codec of the values
 * @param client
 *   the name of the [[dev.coilix.kafka.KafkaClient]] of the cluster with the topic
 * @param settings
 *   how the topic is created
 * @tparam K
 *   the type of the keys
 * @tparam V
 *   the type of the values
 */
case class KafkaTopic[K, V](name: String, keys: TopicCodec[K], values: TopicCodec[V], client: String, settings: TopicSettings)

/** Creation of topics with the given codecs of their keys and values. */
object KafkaTopic {

  /** A topic named `name` on the client `client`, with the given codecs. */
  def apply[K: TopicCodec, V: TopicCodec](
    name:     String,
    client:   String        = KafkaClients.Default,
    settings: TopicSettings = TopicSettings()
  ): KafkaTopic[K, V] =
    KafkaTopic(name, TopicCodec[K], TopicCodec[V], client, settings)
}
