package dev.coilix.kafka

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * The settings of the consumers of a Kafka client. A setting left out keeps the default of fs2-kafka and the Kafka client.
 *
 * @param offsetReset
 *   where a group without a committed offset starts reading
 * @param maxPollRecords
 *   the most records one poll returns
 * @param sessionTimeout
 *   how long the group waits for a silent consumer before it moves its partitions
 * @param heartbeatInterval
 *   how often the consumer tells the group it is alive
 * @param maxPollInterval
 *   the longest time between two polls before the consumer leaves the group
 * @param requestTimeout
 *   how long a request to a broker may take
 * @param pollInterval
 *   how often fs2-kafka polls the brokers
 * @param closeTimeout
 *   how long closing a consumer waits for its commits
 * @param properties
 *   other Kafka consumer properties by their Kafka names, such as `fetch.min.bytes`
 */
case class KafkaConsumerSettings(
  offsetReset:       OffsetReset            = OffsetReset.Earliest,
  maxPollRecords:    Int                    = 500,
  sessionTimeout:    Option[FiniteDuration] = None,
  heartbeatInterval: Option[FiniteDuration] = None,
  maxPollInterval:   Option[FiniteDuration] = None,
  requestTimeout:    Option[FiniteDuration] = None,
  pollInterval:      Option[FiniteDuration] = None,
  closeTimeout:      FiniteDuration         = 10.seconds,
  properties:        Map[String, String]    = Map.empty
) {

  /** The Kafka properties of these settings, without the poll interval and the close timeout, which belong to fs2-kafka. */
  def kafkaProperties: Map[String, String] =
    Map("auto.offset.reset" -> offsetReset.kafkaName, "max.poll.records" -> maxPollRecords.toString) ++ List(
      sessionTimeout.map("session.timeout.ms" -> _.toMillis.toString),
      heartbeatInterval.map("heartbeat.interval.ms" -> _.toMillis.toString),
      maxPollInterval.map("max.poll.interval.ms" -> _.toMillis.toString),
      requestTimeout.map("request.timeout.ms" -> _.toMillis.toString)
    ).flatten                                                                                        ++ properties
}
