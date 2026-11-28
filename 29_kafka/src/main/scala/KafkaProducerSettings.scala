package dev.coilix.kafka

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * The settings of the producer of a Kafka client. A setting left out keeps the default of the Kafka client.
 *
 * @param acks
 *   how many replicas confirm a record before it counts as sent: `all`, `1` or `0`
 * @param linger
 *   how long the producer waits to fill a batch
 * @param batchSize
 *   the size of a batch in bytes
 * @param compression
 *   the compression of batches, such as `lz4` or `zstd`
 * @param deliveryTimeout
 *   how long sending a record may take with its retries
 * @param closeTimeout
 *   how long closing the producer waits for records still in flight; short, so a missing broker does not hold up closing the
 *   application
 * @param properties
 *   other Kafka producer properties by their Kafka names, such as `max.request.size`
 */
case class KafkaProducerSettings(
  acks:            Option[String]         = None,
  linger:          Option[FiniteDuration] = None,
  batchSize:       Int                    = 16384,
  compression:     Option[String]         = None,
  deliveryTimeout: Option[FiniteDuration] = None,
  closeTimeout:    FiniteDuration         = 5.seconds,
  properties:      Map[String, String]    = Map.empty
) {

  /** The Kafka properties of these settings, without the close timeout, which belongs to fs2-kafka. */
  def kafkaProperties: Map[String, String] =
    List(
      acks.map("acks" -> _),
      linger.map("linger.ms" -> _.toMillis.toString),
      compression.map("compression.type" -> _),
      deliveryTimeout.map("delivery.timeout.ms" -> _.toMillis.toString)
    ).flatten.toMap + ("batch.size" -> batchSize.toString) ++ properties
}
