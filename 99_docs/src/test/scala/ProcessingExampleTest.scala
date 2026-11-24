package com.example

import com.example.processing.ShopProcessor
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import dev.coilix.processing.ProcessingSettings
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class ProcessingExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration sets the shards, the assignment and the restart policy") {
    val url      = resources.resolve("guides/processing/application.yaml").toUri.toURL
    val settings = ProcessingSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url))))
    val invoices = settings.of(ShopProcessor.InvoiceExport).resolve("invoice-export", defaultShards = 2, replicaCount = 3)
    val watch    = settings.of(ShopProcessor.OrderWatch).resolve("order-watch", defaultShards = 1, replicaCount = 3)
    assertEquals(settings.shutdownTimeout, 20.seconds)
    assertEquals(
      (invoices.shardCount, invoices.policy.restart.maxAttempts, invoices.policy.restart.backoff.initial),
      (4, 5, 2.seconds)
    )
    assertEquals(watch.assignment.shardsOf(0, 1, 3), List(0))
  }
}
