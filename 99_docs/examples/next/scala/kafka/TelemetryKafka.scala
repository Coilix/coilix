package com.example
package kafka

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.kafka.KafkaClient
import dev.coilix.kafka.KafkaClients

@Configuration
final class TelemetryKafka {

  @Bean
  def telemetry(clients: KafkaClients): KafkaClient = clients.client("telemetry")
}
