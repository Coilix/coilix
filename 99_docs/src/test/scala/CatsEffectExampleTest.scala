package com.example

import dev.coilix.concurrency.CatsEffectSettings
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class CatsEffectExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration sets the limit for finalizers of effectful beans") {
    val url = resources.resolve("guides/cats-effect/application.yaml").toUri.toURL
    assertEquals(
      CatsEffectSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url)))),
      CatsEffectSettings(shutdownTimeout = 1.minute)
    )
  }
}
