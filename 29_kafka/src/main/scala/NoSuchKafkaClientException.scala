package dev.coilix.kafka

import dev.coilix.common.CoilixException

/**
 * Thrown when code asks for a Kafka client that `coilix.kafka.clients` does not configure. The message lists the configured
 * clients.
 *
 * @param name
 *   the name of the missing client
 * @param configured
 *   the names of the configured clients
 */
final class NoSuchKafkaClientException(val name: String, val configured: List[String])
    extends CoilixException(NoSuchKafkaClientException.message(name, configured))

/** The message of [[dev.coilix.kafka.NoSuchKafkaClientException]]. */
object NoSuchKafkaClientException {

  private def message(name: String, configured: List[String]): String =
    if (configured.isEmpty) {
      s"No Kafka client named $name: coilix.kafka.clients configures none"
    } else {
      s"No Kafka client named $name under coilix.kafka.clients; configured: ${configured.mkString(", ")}"
    }
}
