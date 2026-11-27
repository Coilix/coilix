package dev.coilix.kafka
package shop

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Qualifier
import dev.coilix.kafka.KafkaClient
import dev.coilix.kafka.KafkaTopic

@Component
case class PlantMonitor(@Qualifier("telemetry") kafka: KafkaClient) {

  def report(topic: KafkaTopic[String, Long], machine: String, temperature: Long): IO[Unit] =
    kafka.publish(topic, machine, temperature).void
}
