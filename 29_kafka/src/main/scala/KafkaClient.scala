package dev.coilix.kafka

import cats.data.NonEmptySet
import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Slf4s
import fs2.Pipe
import fs2.Stream
import fs2.kafka.CommittableConsumerRecord
import fs2.kafka.CommittableOffset
import fs2.kafka.ConsumerSettings
import fs2.kafka.Deserializer
import fs2.kafka.KafkaConsumer
import fs2.kafka.KafkaProducer
import fs2.kafka.ProducerRecord
import fs2.kafka.ProducerRecords
import fs2.kafka.ProducerSettings
import fs2.kafka.commitBatchWithin
import org.apache.kafka.clients.producer.RecordMetadata

/**
 * One Kafka cluster: publishing to its topics, reading them with consumer groups and committing what was read.
 *
 * The client has one producer, shared by every publication, and opens a consumer for each stream. Records are typed by the
 * [[dev.coilix.kafka.TopicCodec]]s of the [[dev.coilix.kafka.KafkaTopic]]. A stream of a consumer group commits nothing by
 * itself: the offsets of processed records go through [[dev.coilix.kafka.KafkaClient.commits]], which gives at least once
 * processing. The commits run while the stream reads, so a stream that should end stops after `commits`, not before it,
 * because ending the reading closes the consumer with the offsets still waiting for their batch:
 *
 * ```scala
 * client
 *   .subscribe(ShopTopics.orders, group = "invoicing")
 *   .evalMap(record => invoices.create(record.record.value).as(record.offset))
 *   .through(client.commits)
 * ```
 *
 * Clients are created from `coilix.kafka.clients` and found in [[dev.coilix.kafka.KafkaClients]].
 *
 * @param name
 *   the name of the client under `coilix.kafka.clients`
 * @param settings
 *   the settings of the client
 * @param producer
 *   the shared producer of the client
 * @param admin
 *   the admin client of the cluster
 */
@Slf4s
case class KafkaClient(
  name:     String,
  settings: KafkaClientSettings,
  producer: KafkaProducer[IO, Array[Byte], Array[Byte]],
  admin:    KafkaAdmin
) {

  /** Publishes one record and waits until the brokers confirm it. */
  def publish[K, V](topic: KafkaTopic[K, V], key: K, value: V): IO[RecordMetadata] =
    publishAll(topic, List(key -> value)).map(_.head)

  /** Publishes the records in their order and waits until the brokers confirm all of them. */
  def publishAll[K, V](topic: KafkaTopic[K, V], records: List[(K, V)]): IO[List[RecordMetadata]] =
    producer.produce(ProducerRecords(records.map(record(topic)))).flatten.map(_.toList.map(_._2))

  /** Publishes every key and value of the stream and emits the confirmation of each, in order. */
  def publishing[K, V](topic: KafkaTopic[K, V]): Pipe[IO, (K, V), RecordMetadata] =
    _.evalMap((key, value) => publish(topic, key, value))

  /**
   * Reads the topic as a member of the consumer group, which shares the partitions among its members.
   *
   * A record that the codecs of the topic cannot decode fails the stream with a [[dev.coilix.kafka.RecordDecodingException]].
   */
  def subscribe[K, V](topic: KafkaTopic[K, V], group: String): Stream[IO, CommittableConsumerRecord[IO, K, V]] =
    consumerStream(topic, group).evalTap(_.subscribeTo(topic.name)).flatMap(_.records)

  /**
   * Reads the given partitions of the topic, without the balancing of the group, which still keeps the committed offsets.
   * Without partitions the stream is empty.
   */
  def assign[K, V](
    topic:      KafkaTopic[K, V],
    group:      String,
    partitions: Set[Int]
  ): Stream[IO, CommittableConsumerRecord[IO, K, V]] =
    NonEmptySet.fromSet(scala.collection.immutable.SortedSet.from(partitions)).fold(noPartitions(topic, group)) { assigned =>
      consumerStream(topic, group).evalTap(_.assign(topic.name, assigned)).flatMap(_.records)
    }

  /** A consumer of the topic in the group, closed with the resource, for work the streams above do not cover. */
  def consumer[K, V](topic: KafkaTopic[K, V], group: String): Resource[IO, KafkaConsumer[IO, K, V]] =
    KafkaConsumer.resource(consumerSettings(topic, group))

  /** Commits the offsets in batches by the `commit` settings of the client, emitting nothing. */
  def commits: Pipe[IO, CommittableOffset[IO], Unit] =
    commitBatchWithin[IO](settings.commit.maxBatch, settings.commit.maxInterval)

  private def record[K, V](topic: KafkaTopic[K, V])(entry: (K, V)): ProducerRecord[Array[Byte], Array[Byte]] =
    ProducerRecord(topic.name, topic.keys.encode(entry._1), topic.values.encode(entry._2))

  private def consumerStream[K, V](topic: KafkaTopic[K, V], group: String): Stream[IO, KafkaConsumer[IO, K, V]] =
    KafkaConsumer.stream(consumerSettings(topic, group))

  private def consumerSettings[K, V](topic: KafkaTopic[K, V], group: String): ConsumerSettings[IO, K, V] = {
    val base = ConsumerSettings(decoding(topic.name, topic.keys), decoding(topic.name, topic.values))
      .withProperties(settings.commonProperties(name) ++ settings.consumer.kafkaProperties)
      .withGroupId(group)
      .withCloseTimeout(settings.consumer.closeTimeout)
    settings.consumer.pollInterval.fold(base)(base.withPollInterval)
  }

  private def decoding[A](topic: String, codec: TopicCodec[A]): Deserializer[IO, A] =
    Deserializer.instance { (_, _, bytes) =>
      IO.fromEither(codec.decode(Option(bytes).getOrElse(Array.emptyByteArray)).left.map(RecordDecodingException(topic, _)))
    }

  private def noPartitions[K, V](topic: KafkaTopic[K, V], group: String): Stream[IO, CommittableConsumerRecord[IO, K, V]] =
    Stream.exec(logF.warn(s"Group $group reads no partitions of topic ${topic.name} on Kafka client $name"))
}

/** Opening of Kafka clients. */
object KafkaClient {

  /** The client named `name` with its producer and admin client, closed with the resource. */
  def resource(name: String, settings: KafkaClientSettings): Resource[IO, KafkaClient] =
    for {
      producer <- KafkaProducer.resource(producerSettings(name, settings))
      admin    <- KafkaAdmin.resource(name, settings)
    } yield KafkaClient(name, settings, producer, admin)

  private def producerSettings(name: String, settings: KafkaClientSettings): ProducerSettings[IO, Array[Byte], Array[Byte]] =
    ProducerSettings[IO, Array[Byte], Array[Byte]]
      .withProperties(settings.commonProperties(name) ++ settings.producer.kafkaProperties)
      .withCloseTimeout(settings.producer.closeTimeout)
}
