package dev.coilix.kafka

import cats.effect.IO
import cats.effect.Resource
import io.github.embeddedkafka.EmbeddedKafka
import io.github.embeddedkafka.EmbeddedKafkaConfig
import java.util.UUID

/** A Kafka broker inside the test JVM on free ports, and helpers for the tests that use it. */
object KafkaBroker {

  /** Starts a broker and gives its bootstrap servers; the broker stops with the resource. */
  def resource: Resource[IO, String] =
    Resource
      .make(IO.blocking(EmbeddedKafka.start()(using EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0))))(broker =>
        IO.blocking(broker.stop(true))
      )
      .map(broker => s"localhost:${broker.config.kafkaPort}")

  /** A topic name no other test uses. */
  def uniqueName(prefix: String): String = s"$prefix-${UUID.randomUUID()}"

  /** The settings of clients named `names`, all connected to the broker. */
  def settings(servers: String, names: String*): KafkaSettings =
    KafkaSettings(names.map(name => name -> KafkaClientSettings(bootstrapServers = servers)).toMap)
}
