package dev.coilix.kafka

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*

/**
 * Every Kafka client of the application by its name, one per cluster from `coilix.kafka.clients`.
 *
 * A component that works with one cluster takes this bean and picks its client by name, or an application declares beans of
 * the clients it uses, named so that `@Qualifier("telemetry")` finds them:
 *
 * ```scala
 * @Configuration
 * final class KafkaBeans {
 *   @Bean
 *   def telemetry(clients: KafkaClients): KafkaClient = clients.client("telemetry")
 * }
 * ```
 *
 * [[dev.coilix.kafka.KafkaClients.forTopic]] finds the client of a topic, so code that publishes to topics of several clusters
 * needs no names.
 *
 * @param clients
 *   the clients by their names
 */
case class KafkaClients(clients: Map[String, KafkaClient]) {

  /** The client named `name`; throws [[dev.coilix.kafka.NoSuchKafkaClientException]] when it is not configured. */
  def client(name: String): KafkaClient =
    find(name).getOrElse(throw NoSuchKafkaClientException(name, names))

  /** The client named `name`, or `None` when it is not configured. */
  def find(name: String): Option[KafkaClient] = clients.get(name)

  /** The client of the cluster with the topic, by [[dev.coilix.kafka.KafkaTopic.client]]. */
  def forTopic(topic: KafkaTopic[?, ?]): KafkaClient = client(topic.client)

  /** The names of the clients in alphabetical order. */
  def names: List[String] = clients.keys.toList.sorted
}

/** Opening of the clients of the settings. */
object KafkaClients {

  /** The name of the client that topics use unless they name another one. */
  val Default: String = "default"

  /** A client for each entry of the settings, opened in alphabetical order and closed in the reverse order. */
  def resource(settings: KafkaSettings): Resource[IO, KafkaClients] =
    settings.clients.toList
      .sortBy(_._1)
      .traverse((name, client) => KafkaClient.resource(name, client).tupleLeft(name))
      .map(clients => KafkaClients(clients.toMap))
}
