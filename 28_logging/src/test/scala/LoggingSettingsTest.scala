package dev.coilix.logging

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import munit.FunSuite

final class LoggingSettingsTest extends FunSuite {

  private def settingsOf(hocon: String): LoggingSettings =
    LoggingSettings.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon))))

  test("uses info, colors and synchronous writing by default") {
    assertEquals(LoggingSettings.from(Environment.empty), LoggingSettings())
  }

  test("reads the root level, quoted and nested package names and the output") {
    val settings = settingsOf(
      """coilix.logging {
        |  level = warn
        |  levels { "org.apache.kafka" = off, com.example.billing = DEBUG }
        |  colors = false
        |  async = true
        |  queue-size = 16
        |}""".stripMargin
    )
    assertEquals(
      settings,
      LoggingSettings(
        LogLevel.Warn,
        Map("org.apache.kafka" -> LogLevel.Off, "com.example.billing" -> LogLevel.Debug),
        colors    = false,
        async     = true,
        queueSize = 16
      )
    )
  }

  test("rejects an unknown level with the key that has it") {
    val error = intercept[ConfigurationException](settingsOf("""coilix.logging.levels { "org.apache" = loud }"""))
    assert(error.getMessage.contains("coilix.logging.levels.org.apache is 'loud'"), error.getMessage)
  }
}
