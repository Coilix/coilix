package com.example

import dev.coilix.concurrency.FatalErrorAction
import dev.coilix.concurrency.FatalErrorSettings
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class FatalErrorsExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration shuts down with exit code 70 and waits 20 seconds for closing") {
    val url = resources.resolve("guides/fatal-errors/application.yaml").toUri.toURL
    assertEquals(
      FatalErrorSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url)))),
      FatalErrorSettings(FatalErrorAction.Shutdown, 70, 20.seconds)
    )
  }
}
