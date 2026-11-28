package com.example
package kafka

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Qualifier
import dev.coilix.kafka.KafkaClient

@Component
case class PlantMonitor(@Qualifier("telemetry") kafka: KafkaClient) {

  def report(machine: String, temperature: Long): IO[Unit] =
    kafka.admin.createTopics(List(ShopTopics.readings)) *> kafka.publish(ShopTopics.readings, machine, temperature).void
}
