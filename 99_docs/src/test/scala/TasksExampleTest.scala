package com.example

import dev.coilix.concurrency.ConcurrencySettings
import dev.coilix.concurrency.ExecutorSettings
import dev.coilix.concurrency.SchedulerSettings
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class TasksExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration sets the executor and the scheduler") {
    val url = resources.resolve("guides/tasks/application.yaml").toUri.toURL
    assertEquals(
      ConcurrencySettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url)))),
      ConcurrencySettings(
        ExecutorSettings(threadNamePrefix = "report-", virtualThreads = false, poolSize = 4, shutdownTimeout = 1.minute),
        SchedulerSettings(poolSize        = 2)
      )
    )
  }
}
