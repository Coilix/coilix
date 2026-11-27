package com.example

import dev.coilix.concurrency.BlockingSettings
import dev.coilix.concurrency.ComputeSettings
import dev.coilix.concurrency.ConcurrencySettings
import dev.coilix.config.ConfigError.orThrow
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class TasksExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration sets the compute pool and the blocking executor") {
    val url = resources.resolve("guides/tasks/application.yaml").toUri.toURL
    assertEquals(
      ConcurrencySettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url).orThrow))),
      ConcurrencySettings(
        ComputeSettings(threads         = 4),
        BlockingSettings(maxConcurrency = Some(20), shutdownTimeout = 1.minute)
      )
    )
  }
}
