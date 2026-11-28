package dev.coilix.kafka

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * How [[dev.coilix.kafka.KafkaClient.commits]] commits consumed offsets: in batches of at most `maxBatch` offsets, and at
 * least every `maxInterval` while offsets wait.
 *
 * @param maxBatch
 *   the most offsets committed at once
 * @param maxInterval
 *   the longest time an offset waits for its commit
 */
case class KafkaCommitSettings(maxBatch: Int = 500, maxInterval: FiniteDuration = 5.seconds)
