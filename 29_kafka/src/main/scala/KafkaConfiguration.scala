package dev.coilix.kafka

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.config.Environment

/**
 * The beans of the `kafka` module: `kafkaSettings` and `kafkaClients`, with a client for each entry of
 * `coilix.kafka.clients`.
 */
@Configuration
final class KafkaConfiguration {

  /** The settings under `coilix.kafka`, or no clients in a context without an environment. */
  @Bean
  def kafkaSettings(environment: Option[Environment]): KafkaSettings =
    environment.fold(KafkaSettings())(KafkaSettings.from)

  /** The Kafka clients, whose producers, consumers and admin clients close with the context. */
  @Bean
  def kafkaClients(settings: KafkaSettings): Resource[IO, KafkaClients] = KafkaClients.resource(settings)
}
