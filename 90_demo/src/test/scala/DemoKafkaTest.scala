package dev.coilix.demo

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.demo.workshop.CheckInFeed
import dev.coilix.demo.workshop.CheckInLog
import dev.coilix.demo.workshop.WorkshopProcessor
import dev.coilix.demo.workshop.WorkshopTopics
import dev.coilix.kafka.KafkaClients
import dev.coilix.processing.ProcessorError
import dev.coilix.processing.ProcessorManager
import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixIOTestSetup.*
import dev.coilix.testkit.CoilixTestSetup
import io.github.embeddedkafka.EmbeddedKafka
import io.github.embeddedkafka.EmbeddedKafkaConfig
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class DemoKafkaTest extends CatsEffectSuite with CoilixIOSuite {

  override def munitIOTimeout: scala.concurrent.duration.Duration = 90.seconds

  private val broker = ResourceSuiteLocalFixture(
    "kafka",
    Resource
      .make(IO.blocking(EmbeddedKafka.start()(using EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0))))(kafka =>
        IO.blocking(kafka.stop(true))
      )
      .map(kafka => s"localhost:${kafka.config.kafkaPort}")
  )

  override def munitFixtures: Seq[munit.AnyFixture[?]] = List(broker)

  private def setup: CoilixTestSetup =
    CoilixTestSetup
      .forApplication[DemoApplication.type]
      .withProperties("coilix.kafka.clients.default.bootstrap-servers" -> broker())
      .withIORuntime(munitIORuntime)

  test("creates the check-in topic when the workshop opens") {
    setup.useIO {
      bean[KafkaClients].client("default").admin.partitionsOf(WorkshopTopics.checkIns.name).map(_.size).assertEquals(3)
    }
  }

  test("announces the batch of checked-in vehicles on Kafka when the check-in stops") {
    setup.useIO {
      val kafka = bean[KafkaClients].forTopic(WorkshopTopics.checkIns)
      for {
        _         <- arrivals(bean[CheckInLog])
        _         <- ProcessorError.raised(bean[ProcessorManager].stop(WorkshopProcessor.VehicleCheckIn))
        announced <- kafka
                       .subscribe(WorkshopTopics.checkIns, "owner-app")
                       .scan(Set.empty[String])(_ + _.record.key)
                       .find(_ == fleet)
                       .compile
                       .last
      } yield assertEquals(announced, Some(fleet))
    }
  }

  test("announces nothing without a Kafka client") {
    CoilixTestSetup.forApplication[DemoApplication.type].withIORuntime(munitIORuntime).useIO {
      bean[CheckInFeed].announce(bean[CheckInLog].arrivals).map(_ => assertEquals(bean[KafkaClients].names, Nil))
    }
  }

  private val fleet = Set("WVWZZZ1KZ6W000001", "TMBJJ7NE8J0000002")

  private def arrivals(checkIns: CheckInLog): IO[Unit] =
    IO(checkIns.arrivals.size == 2).flatMap(done => if (done) IO.unit else IO.sleep(10.millis) *> arrivals(checkIns))
}
