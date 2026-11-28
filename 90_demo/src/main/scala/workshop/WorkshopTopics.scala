package dev.coilix.demo
package workshop

import dev.coilix.kafka.KafkaTopic
import dev.coilix.kafka.TopicSettings

/** The Kafka topics of the workshop. */
object WorkshopTopics {

  /** The vehicles checked in by [[dev.coilix.demo.workshop.VehicleCheckIn]]: the VIN as the key and the bay as the value. */
  val checkIns: KafkaTopic[String, String] = KafkaTopic("workshop.check-ins", settings = TopicSettings(partitions = 3))
}
