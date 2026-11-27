package dev.coilix.kafka

import cats.effect.IO
import dev.coilix.config.valuetype.Secret
import dev.coilix.kafka.shop.PlantMonitor
import dev.coilix.testkit.CoilixIOSuite
import dev.coilix.testkit.CoilixIOTestSetup.*
import dev.coilix.testkit.CoilixTestSetup
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

class KafkaModuleTest extends CatsEffectSuite with CoilixIOSuite {

  override def munitIOTimeout: scala.concurrent.duration.Duration = 90.seconds

  private val broker = ResourceSuiteLocalFixture("kafka", KafkaBroker.resource)

  override def munitFixtures: Seq[munit.AnyFixture[?]] = List(broker)

  private def setup: CoilixTestSetup =
    CoilixTestSetup
      .forPackages("dev.coilix.kafka.shop")
      .withProperties(
        "coilix.kafka.clients.default.bootstrap-servers"           -> broker(),
        "coilix.kafka.clients.telemetry.bootstrap-servers"         -> broker(),
        "coilix.kafka.clients.telemetry.client-id"                 -> "plant-7",
        "coilix.kafka.clients.telemetry.consumer.max-poll-records" -> "200",
        "coilix.kafka.clients.telemetry.commit.max-batch"          -> "1000",
        "coilix.kafka.clients.telemetry.auth.sasl-jaas-config"     -> "jaas"
      )
      .withIORuntime(munitIORuntime)

  test("binds a client for each entry of coilix.kafka.clients") {
    setup.useIO {
      IO {
        val telemetry = bean[KafkaSettings].clients("telemetry")
        assertEquals(bean[KafkaClients].names, List("default", "telemetry"))
        assertEquals(telemetry.clientId, Some("plant-7"))
        assertEquals(telemetry.consumer.maxPollRecords, 200)
        assertEquals(telemetry.commit, KafkaCommitSettings(maxBatch = 1000))
        assertEquals(telemetry.auth.saslJaasConfig, Some(Secret("jaas")))
      }
    }
  }

  test("binds the security protocol written in kebab case") {
    val jaas = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"plant\" password=\"secret\";"
    setup
      .withProperties(
        "coilix.kafka.clients.telemetry.auth.security-protocol" -> "sasl-plaintext",
        "coilix.kafka.clients.telemetry.auth.sasl-mechanism"    -> "PLAIN",
        "coilix.kafka.clients.telemetry.auth.sasl-jaas-config"  -> jaas
      )
      .useIO {
        IO(assertEquals(bean[KafkaSettings].clients("telemetry").auth.securityProtocol, SecurityProtocol.SaslPlaintext))
      }
  }

  test("lets the application inject a client by its name") {
    val readings = KafkaTopic[String, Long](KafkaBroker.uniqueName("readings"), client = "telemetry")
    setup.useIO {
      val kafka = bean[KafkaClients].forTopic(readings)
      kafka.admin.createTopics(List(readings)) *> bean[PlantMonitor].report(readings, "press-3", 81L) *>
        kafka.subscribe(readings, "dashboard").take(1).compile.lastOrError.map { record =>
          assertEquals(bean[PlantMonitor].kafka.name, "telemetry")
          assertEquals(record.record.key -> record.record.value, "press-3" -> 81L)
        }
    }
  }

  test("has no clients without coilix.kafka.clients") {
    CoilixTestSetup.forPackages("dev.coilix.kafka.none").withIORuntime(munitIORuntime).useIO {
      IO(assertEquals(bean[KafkaClients].names, Nil))
    }
  }
}
