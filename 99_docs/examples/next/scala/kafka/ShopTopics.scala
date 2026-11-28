package com.example
package kafka

import dev.coilix.kafka.CleanupPolicy
import dev.coilix.kafka.KafkaTopic
import dev.coilix.kafka.TopicSettings

object ShopTopics {

  val orders: KafkaTopic[String, String] = KafkaTopic("shop.orders", settings = TopicSettings(partitions = 6))

  val stock: KafkaTopic[String, Long] =
    KafkaTopic("shop.stock", settings = TopicSettings(cleanupPolicy = CleanupPolicy.Compact, retention = None))

  val readings: KafkaTopic[String, Long] = KafkaTopic("plant.readings", client = "telemetry")
}
