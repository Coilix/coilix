package dev.coilix.kafka

import dev.coilix.config.valuetype.Secret
import munit.FunSuite
import scala.concurrent.duration.DurationInt

class KafkaPropertiesTest extends FunSuite {

  test("gives the bootstrap servers, the client id and the security protocol to every part of a client") {
    val settings = KafkaClientSettings(bootstrapServers = "k1:9092,k2:9092")
    assertEquals(
      settings.commonProperties("telemetry"),
      Map("bootstrap.servers" -> "k1:9092,k2:9092", "client.id" -> "telemetry", "security.protocol" -> "PLAINTEXT")
    )
    assertEquals(settings.copy(clientId = Some("plant-7")).commonProperties("telemetry")("client.id"), "plant-7")
  }

  test("turns the credentials into SASL and SSL properties") {
    val auth = KafkaAuthSettings(
      securityProtocol      = SecurityProtocol.SaslSsl,
      saslMechanism         = Some("SCRAM-SHA-512"),
      saslJaasConfig        = Some(Secret("jaas")),
      sslTruststoreLocation = Some("/etc/kafka/trust.p12"),
      sslTruststorePassword = Some(Secret("changeit"))
    )
    assertEquals(
      auth.properties,
      Map(
        "security.protocol"       -> "SASL_SSL",
        "sasl.mechanism"          -> "SCRAM-SHA-512",
        "sasl.jaas.config"        -> "jaas",
        "ssl.truststore.location" -> "/etc/kafka/trust.p12",
        "ssl.truststore.password" -> "changeit"
      )
    )
  }

  test("gives only the set consumer settings, with other properties winning") {
    val consumer = KafkaConsumerSettings(
      offsetReset    = OffsetReset.Latest,
      maxPollRecords = 200,
      sessionTimeout = Some(45.seconds),
      properties     = Map("fetch.min.bytes" -> "1024", "max.poll.records" -> "100")
    )
    assertEquals(
      consumer.kafkaProperties,
      Map(
        "auto.offset.reset"  -> "latest",
        "max.poll.records"   -> "100",
        "session.timeout.ms" -> "45000",
        "fetch.min.bytes"    -> "1024"
      )
    )
  }

  test("gives only the set producer settings") {
    assertEquals(
      KafkaProducerSettings(acks = Some("all"), linger = Some(20.millis), compression = Some("zstd")).kafkaProperties,
      Map("acks" -> "all", "linger.ms" -> "20", "compression.type" -> "zstd", "batch.size" -> "16384")
    )
    assertEquals(KafkaProducerSettings().kafkaProperties, Map("batch.size" -> "16384"))
  }

  test("gives the timeouts of the admin client and the properties of a created topic") {
    assertEquals(
      KafkaAdminSettings(timeout = 3.seconds).kafkaProperties,
      Map("default.api.timeout.ms" -> "3000", "request.timeout.ms" -> "3000")
    )
    assertEquals(
      TopicSettings(cleanupPolicy = CleanupPolicy.Compact, retention = None, retentionBytes = Some(1024L)).kafkaProperties(2),
      Map("cleanup.policy" -> "compact", "retention.ms" -> "-1", "min.insync.replicas" -> "2", "retention.bytes" -> "1024")
    )
  }
}
