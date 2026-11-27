package dev.coilix.kafka

import cats.effect.IO
import cats.effect.Resource
import munit.CatsEffectSuite
import org.apache.kafka.common.TopicPartition
import scala.concurrent.duration.DurationInt

class KafkaAdminTest extends CatsEffectSuite {

  override def munitIOTimeout: scala.concurrent.duration.Duration = 90.seconds

  private val broker = ResourceSuiteLocalFixture("kafka", KafkaBroker.resource)

  override def munitFixtures: Seq[munit.AnyFixture[?]] = List(broker)

  private def client: Resource[IO, KafkaClient] =
    KafkaClients.resource(KafkaBroker.settings(broker(), KafkaClients.Default)).map(_.client(KafkaClients.Default))

  private def topic(partitions: Int): KafkaTopic[String, String] =
    KafkaTopic(KafkaBroker.uniqueName("readings"), settings = TopicSettings(partitions = partitions))

  test("creates topics with their partitions and skips the existing ones") {
    val readings = topic(partitions = 3)
    client.use { kafka =>
      for {
        _          <- kafka.admin.createTopics(List(readings))
        _          <- kafka.admin.createTopics(List(readings))
        names      <- kafka.admin.topicNames
        partitions <- kafka.admin.partitionsOf(readings.name)
      } yield {
        assert(names.contains(readings.name), names)
        assertEquals(partitions, (0 until 3).map(TopicPartition(readings.name, _)).toSet)
      }
    }
  }

  test("gives no partitions of a missing topic") {
    client.use(_.admin.partitionsOf(KafkaBroker.uniqueName("missing")).assertEquals(Set.empty))
  }

  test("gives the earliest and the latest offsets of partitions") {
    val readings  = topic(partitions = 1)
    val partition = TopicPartition(readings.name, 0)
    client.use { kafka =>
      for {
        _        <- kafka.admin.createTopics(List(readings))
        _        <- kafka.publishAll(readings, List("a" -> "1", "b" -> "2"))
        earliest <- kafka.admin.earliestOffsets(Set(partition))
        latest   <- kafka.admin.latestOffsets(Set(partition))
      } yield assertEquals((earliest, latest), (Map(partition -> 0L), Map(partition -> 2L)))
    }
  }

  test("sets and reads the committed offsets of a group") {
    val readings = topic(partitions = 2)
    val first    = TopicPartition(readings.name, 0)
    val second   = TopicPartition(readings.name, 1)
    client.use { kafka =>
      for {
        _         <- kafka.admin.createTopics(List(readings))
        _         <- kafka.admin.alterOffsets("metering", Map(first -> 0L))
        committed <- kafka.admin.committedOffsets("metering", Set(first, second))
      } yield assertEquals(committed, Map(first -> 0L))
    }
  }

  test("gives no offsets of a group without commits and ignores empty requests") {
    client.use { kafka =>
      for {
        unknown <- kafka.admin.committedOffsets(KafkaBroker.uniqueName("group"), Set(TopicPartition("any", 0)))
        none    <- kafka.admin.committedOffsets("metering", Set.empty)
        offsets <- kafka.admin.earliestOffsets(Set.empty)
        _       <- kafka.admin.alterOffsets("metering", Map.empty)
      } yield assertEquals((unknown, none, offsets), (Map.empty, Map.empty, Map.empty))
    }
  }

  test("deletes consumer groups and leaves a missing one with a message") {
    val readings  = topic(partitions = 1)
    val partition = TopicPartition(readings.name, 0)
    val group     = KafkaBroker.uniqueName("legacy")
    client.use { kafka =>
      for {
        _         <- kafka.admin.createTopics(List(readings))
        _         <- kafka.admin.alterOffsets(group, Map(partition -> 0L))
        _         <- kafka.admin.deleteGroups(List(group, KafkaBroker.uniqueName("missing")))
        committed <- kafka.admin.committedOffsets(group, Set(partition))
      } yield assertEquals(committed, Map.empty)
    }
  }
}
