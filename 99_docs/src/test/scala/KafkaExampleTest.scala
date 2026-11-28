package com.example

import com.example.kafka.ShopTopics
import dev.coilix.config.ConfigError.orThrow
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import dev.coilix.kafka.KafkaClients
import dev.coilix.kafka.KafkaSettings
import dev.coilix.kafka.SecurityProtocol
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class KafkaExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  private def settings: KafkaSettings = {
    val url = resources.resolve("guides/kafka/application.yaml").toUri.toURL
    KafkaSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url).orThrow)))
  }

  test("the guide configuration has a client for each cluster") {
    val clients = settings.clients
    assertEquals(clients.keySet, Set(KafkaClients.Default, "telemetry"))
    assertEquals(
      (
        clients("default").consumer.maxPollRecords,
        clients("default").commit.maxInterval,
        clients("default").admin.replicationFactor
      ),
      (200, 2.seconds, 3)
    )
    assertEquals(
      (clients("telemetry").auth.securityProtocol, clients("telemetry").producer.linger),
      (SecurityProtocol.SaslSsl, Some(20.millis))
    )
  }

  test("the topics of the guide name the clients of their clusters") {
    assertEquals(List(ShopTopics.orders, ShopTopics.readings).map(_.client), List("default", "telemetry"))
  }
}
