package dev.coilix.processing

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class ProcessingSettingsTest extends FunSuite {

  private def settingsOf(hocon: String): ProcessingSettings =
    ProcessingSettings.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon))))

  test("uses the defaults of Coilix without configuration") {
    val resolved = ProcessingSettings().of(TestProcessor.Ticker).resolve("ticker", defaultShards = 3, replicaCount = 1)
    assertEquals(
      (resolved.shardCount, resolved.slotCount, resolved.assignment.name, resolved.enabled),
      (3, 256, "round-robin", true)
    )
    assertEquals(resolved.policy, SupervisionPolicy())
  }

  test("takes processor keys over the code and the code over the defaults") {
    val settings = settingsOf(
      """coilix.processors {
        |  autostart = false
        |  shutdown-timeout = 5s
        |  defaults { slots = 64, assignment = range, restart { max-attempts = 4, backoff.initial = 2s } }
        |  ticker { shards = 8, restart.max-attempts = 7 }
        |}""".stripMargin
    )
    val ticker   = settings.of(TestProcessor.Ticker).resolve("ticker", defaultShards = 2, replicaCount = 1)
    val exporter = settings.of(TestProcessor.Exporter).resolve("exporter", defaultShards = 2, replicaCount = 1)
    assertEquals((settings.autostart, settings.shutdownTimeout), (false, 5.seconds))
    assertEquals((ticker.shardCount, ticker.slotCount, ticker.assignment.name), (8, 64, "range"))
    assertEquals((ticker.policy.restart.maxAttempts, ticker.policy.restart.backoff.initial), (7, 2.seconds))
    assertEquals((exporter.shardCount, exporter.policy.restart.maxAttempts), (2, 4))
  }

  test("reads the completion policy and the pinned replica") {
    val settings = settingsOf(
      "coilix.processors.exporter { on-completion = restart, completion-delay = 10s, assignment = pinned, pinned-replica = 1 }"
    )
    val resolved = settings.of(TestProcessor.Exporter).resolve("exporter", defaultShards = 1, replicaCount = 2)
    assertEquals(resolved.policy.completion, CompletionPolicy.Restart(10.seconds))
    assertEquals(resolved.assignment.replicasOf(0, 1, 2), List(1))
  }

  test("reads drain-timeout per processor with the defaults as fallback") {
    val settings = settingsOf("coilix.processors { defaults.drain-timeout = 20s, exporter.drain-timeout = 3s }")
    val drains   = List(TestProcessor.Ticker, TestProcessor.Exporter).map(id =>
      settings.of(id).resolve(id.name, defaultShards = 1, replicaCount = 1).drainTimeout
    )
    assertEquals(
      (drains, ProcessingSettings().of(TestProcessor.Ticker).resolve("ticker", 1, 1).drainTimeout),
      (List(Some(20.seconds), Some(3.seconds)), None)
    )
  }

  test("reports the configured names that belong to no processor") {
    val settings = settingsOf("coilix.processors { tikcer.shards = 2, ticker.shards = 2 }")
    assertEquals(settings.unknown(List(TestProcessor.Ticker)), List("tikcer"))
  }

  test("rejects values out of range") {
    def rejected(hocon: String, replicas: Int = 1): String =
      intercept[ConfigurationException](settingsOf(hocon).of(TestProcessor.Ticker).resolve("ticker", 1, replicas)).getMessage
    assertEquals(rejected("coilix.processors.ticker.shards = 0"), "Processor ticker has 0 shards, at least 1 is needed")
    assertEquals(
      rejected("coilix.processors.ticker { assignment = pinned, pinned-replica = 2 }", replicas = 2).take(36),
      "Processor ticker is pinned to replic"
    )
    assert(rejected("coilix.processors.ticker.on-completion = later").contains("unknown on-completion 'later'"))
    assert(rejected("coilix.processors.ticker.drain-timeout = -2s").endsWith("it must be positive"))
  }
}
