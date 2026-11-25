package com.example

import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import dev.coilix.diagnostics.DeadlockAction
import dev.coilix.diagnostics.DiagnosticsSettings
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class DiagnosticsExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration checks every 5 seconds and shuts down after a deadlock") {
    val url      = resources.resolve("guides/diagnostics/application.yaml").toUri.toURL
    val settings = DiagnosticsSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url))))
    assertEquals(
      (settings.interval, settings.threads.onDeadlock, settings.threads.blockedAfter, settings.threads.warningCount),
      (5.seconds, DeadlockAction.Shutdown, 30.seconds, 1000)
    )
  }
}
