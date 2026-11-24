package com.example

import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import dev.coilix.pekko.PekkoSettings
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class PekkoExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration sets the name, the shutdown limit and the configuration of Pekko") {
    val url      = resources.resolve("guides/pekko/application.yaml").toUri.toURL
    val settings = PekkoSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url))))
    assertEquals(
      (settings.systemName, settings.shutdownTimeout, settings.config.getInt("pekko.stream.materializer.max-input-buffer-size")),
      ("shop", 20.seconds, 32)
    )
  }
}
