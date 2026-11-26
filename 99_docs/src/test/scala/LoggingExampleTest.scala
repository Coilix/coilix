package com.example

import dev.coilix.config.ConfigError.orThrow
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import dev.coilix.logging.LogLevel
import dev.coilix.logging.LoggingSettings
import java.nio.file.Paths

class LoggingExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration sets the root level, the levels by package and asynchronous writing") {
    val url      = resources.resolve("guides/logging/application.yaml").toUri.toURL
    val settings = LoggingSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url).orThrow)))
    assertEquals(
      (settings.level, settings.levels, settings.async),
      (
        LogLevel.Info,
        Map(
          "org.apache.kafka"             -> LogLevel.Warn,
          "com.example.noisy"            -> LogLevel.Off,
          "com.example.billing.Invoices" -> LogLevel.Debug
        ),
        true
      )
    )
  }
}
