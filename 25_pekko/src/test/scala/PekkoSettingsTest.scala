package dev.coilix.pekko

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class PekkoSettingsTest extends FunSuite {

  private def settings(hocon: String): PekkoSettings =
    PekkoSettings.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon))))

  test("uses the defaults without coilix.pekko") {
    assertEquals(PekkoSettings.from(Environment.empty), PekkoSettings())
  }

  test("reads the name, the shutdown limit and the configuration of Pekko") {
    val read = settings(
      """coilix.pekko { system-name = shop, shutdown-timeout = 5s, config { pekko.loglevel = DEBUG } }"""
    )
    assertEquals(
      (read.systemName, read.shutdownTimeout, read.config.getString("pekko.loglevel")),
      ("shop", 5.seconds, "DEBUG")
    )
  }

  test("rejects a configuration of Pekko that is not a section") {
    intercept[ConfigurationException](settings("coilix.pekko.config = 3"))
  }
}
