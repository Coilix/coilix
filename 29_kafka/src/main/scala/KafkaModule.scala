package dev.coilix.kafka

import dev.coilix.context.CoilixModule

/**
 * Adds the [[dev.coilix.kafka.KafkaClients]] of `coilix.kafka.clients` when `coilix-kafka` is on the classpath.
 *
 * It can be disabled with `coilix.modules.kafka.enabled = false`.
 */
final class KafkaModule extends CoilixModule {

  override def name: String = "kafka"

  override def components: List[Class[?]] = List(classOf[KafkaConfiguration])
}
