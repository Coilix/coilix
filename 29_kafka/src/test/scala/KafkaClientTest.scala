package dev.coilix.kafka

import cats.effect.IO
import cats.effect.Resource
import munit.CatsEffectSuite
import org.apache.kafka.common.TopicPartition
import scala.concurrent.duration.DurationInt

class KafkaClientTest extends CatsEffectSuite {

  override def munitIOTimeout: scala.concurrent.duration.Duration = 90.seconds

  private val broker = ResourceSuiteLocalFixture("kafka", KafkaBroker.resource)

  override def munitFixtures: Seq[munit.AnyFixture[?]] = List(broker)

  private def clients: Resource[IO, KafkaClients] =
    KafkaClients.resource(KafkaBroker.settings(broker(), KafkaClients.Default, "backup"))

  private def client: Resource[IO, KafkaClient] = clients.map(_.client(KafkaClients.Default))

  private def batchingClient: Resource[IO, KafkaClient] = {
    val settings = KafkaClientSettings(bootstrapServers = broker(), commit = KafkaCommitSettings(maxBatch = 3))
    KafkaClient.resource(KafkaClients.Default, settings)
  }

  private def topic(partitions: Int = 1): KafkaTopic[String, String] =
    KafkaTopic(KafkaBroker.uniqueName("orders"), settings = TopicSettings(partitions = partitions))

  test("reads the published records as a member of a group and commits their offsets") {
    val orders = topic()
    batchingClient.use { kafka =>
      for {
        _         <- kafka.admin.createTopics(List(orders))
        _         <- kafka.publishAll(orders, List("a" -> "first", "b" -> "second", "c" -> "third"))
        seen      <- IO.ref(List.empty[fs2.kafka.CommittableConsumerRecord[IO, String, String]])
        _         <- kafka.subscribe(
                       orders,
                       "invoicing"
                     ).evalTap(r => seen.update(_ :+ r)).map(_.offset).through(kafka.commits).take(1).compile.drain
        read      <- seen.get
        committed <- kafka.admin.committedOffsets("invoicing", Set(TopicPartition(orders.name, 0)))
      } yield {
        assertEquals(
          read.map(record => record.record.key -> record.record.value),
          List("a" -> "first", "b" -> "second", "c" -> "third")
        )
        assertEquals(committed, Map(TopicPartition(orders.name, 0) -> 3L))
      }
    }
  }

  test("starts a group after its committed offset") {
    val orders = topic()
    client.use { kafka =>
      for {
        _    <- kafka.admin.createTopics(List(orders))
        _    <- kafka.publishAll(orders, List("a" -> "old", "b" -> "new"))
        _    <- kafka.admin.alterOffsets("invoicing", Map(TopicPartition(orders.name, 0) -> 1L))
        read <- kafka.subscribe(orders, "invoicing").take(1).compile.toList
      } yield assertEquals(read.map(_.record.value), List("new"))
    }
  }

  test("reads only the assigned partitions") {
    val orders = topic(partitions = 3)
    client.use { kafka =>
      for {
        _      <- kafka.admin.createTopics(List(orders))
        _      <- kafka.publishAll(orders, (1 to 30).toList.map(i => s"key-$i" -> s"order-$i"))
        latest <- kafka.admin.latestOffsets(Set(TopicPartition(orders.name, 1)))
        count   = latest(TopicPartition(orders.name, 1))
        read   <- kafka.assign(orders, "invoicing", Set(1)).take(count).compile.toList
      } yield {
        assert(count > 0, "partition 1 got no records")
        assertEquals(read.map(_.record.partition).distinct, List(1))
      }
    }
  }

  test("reads nothing without assigned partitions") {
    client.use(kafka => kafka.assign(topic(), "invoicing", Set.empty).compile.toList.assertEquals(Nil))
  }

  test("fails the stream with the topic when a value cannot be decoded") {
    val texts   = topic()
    val numbers = KafkaTopic[String, Long](texts.name)
    client.use { kafka =>
      kafka.admin.createTopics(List(texts)) *> kafka.publish(texts, "a", "not a number") *>
        kafka.subscribe(numbers, "metering").take(1).compile.drain.intercept[RecordDecodingException].map { error =>
          assertEquals(error.topic, texts.name)
          assert(error.getMessage.contains("expected 8 bytes"), error.getMessage)
        }
    }
  }

  test("publishes a stream and emits the confirmations in order") {
    val orders = topic()
    client.use { kafka =>
      kafka.admin.createTopics(List(orders)) *>
        fs2.Stream("a" -> "1", "b" -> "2").through(kafka.publishing(orders)).compile.toList.map { confirmations =>
          assertEquals(confirmations.map(_.offset), List(0L, 1L))
        }
    }
  }

  test("gives a consumer for work the streams do not cover") {
    val orders = topic()
    client.use { kafka =>
      kafka.admin.createTopics(List(orders)) *> kafka.consumer(orders, "audit").use { consumer =>
        consumer.partitionsFor(orders.name).map(partitions => assertEquals(partitions.map(_.partition()), List(0)))
      }
    }
  }

  test("finds the client of a topic by its name") {
    clients.use { kafka =>
      IO {
        assertEquals(kafka.forTopic(KafkaTopic[String, String]("audit", client = "backup")).name, "backup")
        assertEquals(kafka.names, List("backup", "default"))
      }
    }
  }

  test("names the configured clients when a client is missing") {
    clients.use { kafka =>
      IO(intercept[NoSuchKafkaClientException](kafka.client("telemetry"))).map { error =>
        assertEquals(error.getMessage, "No Kafka client named telemetry under coilix.kafka.clients; configured: backup, default")
      }
    }
  }
}
