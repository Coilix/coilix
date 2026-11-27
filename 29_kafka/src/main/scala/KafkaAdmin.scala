package dev.coilix.kafka

import cats.effect.IO
import cats.effect.Outcome
import cats.effect.Resource
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import java.time.Duration
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.GroupIdNotFoundException
import org.apache.kafka.common.errors.TopicExistsException
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

/**
 * The admin operations of one Kafka cluster: topics, partitions, offsets and consumer groups.
 *
 * Every operation waits at most the `timeout` of the [[dev.coilix.kafka.KafkaAdminSettings]] and logs a warning when it takes
 * longer than `warn-if-slower-than`. A missing topic gives no partitions and a missing group no offsets, instead of a
 * failure. Each [[dev.coilix.kafka.KafkaClient]] has one, opened and closed with it.
 *
 * @param client
 *   the name of the client, used in log messages
 * @param settings
 *   the timeouts and the settings of created topics
 * @param admin
 *   the admin client of Kafka
 */
@Slf4s
case class KafkaAdmin(client: String, settings: KafkaAdminSettings, admin: Admin) {

  /** The names of the topics, without the internal ones of Kafka. */
  def topicNames: IO[Set[String]] =
    awaitKafka("listTopics", admin.listTopics().names()).map(_.asScala.toSet)

  /** Creates the topics that do not exist yet with their [[dev.coilix.kafka.TopicSettings]]. */
  def createTopics(topics: List[KafkaTopic[?, ?]]): IO[Unit] =
    topics.traverse_(createTopic)

  /** The partitions of the topic, or none when it does not exist. */
  def partitionsOf(topic: String): IO[Set[TopicPartition]] =
    awaitKafka(s"describeTopics($topic)", admin.describeTopics(List(topic).asJava).allTopicNames())
      .map(_.asScala.get(topic).fold(Set.empty)(_.partitions().asScala.map(p => TopicPartition(topic, p.partition())).toSet))
      .recover { case _: UnknownTopicOrPartitionException => Set.empty }

  /** The offset of the oldest record still kept in each partition. */
  def earliestOffsets(partitions: Set[TopicPartition]): IO[Map[TopicPartition, Long]] =
    offsets("earliestOffsets", partitions, OffsetSpec.earliest())

  /** The offset after the newest record of each partition. */
  def latestOffsets(partitions: Set[TopicPartition]): IO[Map[TopicPartition, Long]] =
    offsets("latestOffsets", partitions, OffsetSpec.latest())

  /** The offsets the group committed in the given partitions; partitions without a commit are left out. */
  def committedOffsets(group: String, partitions: Set[TopicPartition]): IO[Map[TopicPartition, Long]] =
    if (partitions.isEmpty) {
      IO.pure(Map.empty)
    } else {
      val spec = ListConsumerGroupOffsetsSpec().topicPartitions(partitions.toList.asJava)
      awaitKafka(
        s"committedOffsets($group)",
        admin.listConsumerGroupOffsets(Map(group -> spec).asJava).partitionsToOffsetAndMetadata(group)
      ).map(committed(partitions)).recover { case _: GroupIdNotFoundException => Map.empty }
    }

  /** Sets the committed offsets of the group, which must have no active members. */
  def alterOffsets(group: String, offsets: Map[TopicPartition, Long]): IO[Unit] =
    if (offsets.isEmpty) {
      IO.unit
    } else {
      val metadata = offsets.view.mapValues(OffsetAndMetadata(_)).toMap.asJava
      awaitKafka(s"alterOffsets($group)", admin.alterConsumerGroupOffsets(group, metadata).all()) *>
        logF.info(s"Set ${offsets.size} offsets of group $group on Kafka client $client: ${describe(offsets)}")
    }

  /** Deletes the consumer groups; a group that still has members is left with a message in the log. */
  def deleteGroups(groups: List[String]): IO[Unit] =
    groups.traverse_(deleteGroup)

  private def createTopic(topic: KafkaTopic[?, ?]): IO[Unit] =
    awaitKafka(s"createTopic(${topic.name})", admin.createTopics(List(newTopic(topic)).asJava).all())
      .flatMap(_ => logF.info(s"Created topic ${topic.name} on Kafka client $client"))
      .recover { case _: TopicExistsException => () }

  private def newTopic(topic: KafkaTopic[?, ?]): NewTopic =
    NewTopic(topic.name, topic.settings.partitions, settings.replicationFactor.toShort)
      .configs(topic.settings.kafkaProperties(settings.minInSyncReplicas).asJava)

  private def offsets(operation: String, partitions: Set[TopicPartition], spec: OffsetSpec): IO[Map[TopicPartition, Long]] =
    if (partitions.isEmpty) {
      IO.pure(Map.empty)
    } else {
      val request = partitions.map(_ -> spec).toMap.asJava
      awaitKafka(operation, admin.listOffsets(request).all()).map(_.asScala.view.mapValues(_.offset()).toMap)
    }

  private def committed(
    partitions: Set[TopicPartition]
  )(offsets: java.util.Map[TopicPartition, OffsetAndMetadata]): Map[TopicPartition, Long] =
    offsets.asScala.toMap.collect {
      case (partition, metadata) if Option(metadata).isDefined && partitions.contains(partition) => partition -> metadata.offset()
    }

  private def deleteGroup(group: String): IO[Unit] =
    awaitKafka(s"deleteGroup($group)", admin.deleteConsumerGroups(List(group).asJava).all())
      .flatMap(_ => logF.info(s"Deleted consumer group $group on Kafka client $client"))
      .handleErrorWith(failure =>
        logF.info(s"Consumer group $group on Kafka client $client was not deleted: ${failure.getMessage}")
      )

  private def describe(offsets: Map[TopicPartition, Long]): String =
    offsets.toList.sortBy(_._1.toString).map((partition, offset) => s"$partition=$offset").mkString(", ")

  private def awaitKafka[A](operation: String, future: KafkaFuture[A]): IO[A] =
    IO.monotonic.flatMap { start =>
      IO.fromCompletableFuture(IO(future.toCompletionStage.toCompletableFuture))
        .timeout(settings.timeout)
        .guaranteeCase(outcome => IO.monotonic.flatMap(end => warnIfSlow(operation, end - start, outcome)))
    }

  private def warnIfSlow[A](operation: String, took: FiniteDuration, outcome: Outcome[IO, Throwable, A]): IO[Unit] =
    if (took >= settings.warnIfSlowerThan) {
      logF.warn(s"Kafka admin operation $operation on client $client took ${took.toMillis} ms (${status(outcome)})")
    } else {
      IO.unit
    }

  private def status[A](outcome: Outcome[IO, Throwable, A]): String =
    outcome match {
      case Outcome.Succeeded(_) => "succeeded"
      case Outcome.Errored(e)   => s"failed with ${e.getClass.getSimpleName}"
      case Outcome.Canceled()   => "cancelled"
    }
}

/** Opening of admin clients. */
object KafkaAdmin {

  /** The admin client of the Kafka client named `name`, closed with the resource. */
  def resource(name: String, client: KafkaClientSettings): Resource[IO, KafkaAdmin] =
    Resource
      .make(IO.blocking(Admin.create(properties(name, client))))(admin =>
        IO.blocking(admin.close(Duration.ofMillis(client.admin.timeout.toMillis)))
      )
      .map(KafkaAdmin(name, client.admin, _))

  private def properties(name: String, client: KafkaClientSettings): java.util.Map[String, AnyRef] =
    (client.commonProperties(name) ++ client.admin.kafkaProperties).view.mapValues(value => value: AnyRef).toMap.asJava
}
