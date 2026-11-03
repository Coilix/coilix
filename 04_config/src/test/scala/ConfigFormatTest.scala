package dev.coilix.config

import munit.FunSuite
import scala.jdk.CollectionConverters.*

final class ConfigFormatTest extends FunSuite {
  private def resource(name: String) = getClass.getClassLoader.getResource(name)

  test("parses HOCON") {
    val config = HoconConfigFormat().parse(resource("formats/sample.conf"))
    assertEquals((config.getString("app.name"), config.getStringList("app.servers").asScala.toList), ("conf", List("a", "b")))
  }

  test("parses properties") {
    val config = PropertiesConfigFormat().parse(resource("formats/sample.properties"))
    assertEquals((config.getString("app.name"), config.getStringList("app.servers").asScala.toList), ("props", List("e", "f")))
  }

  test("parses YAML") {
    val config = YamlConfigFormat().parse(resource("formats/sample.yaml"))
    assertEquals((config.getInt("app.port"), config.getStringList("app.servers").asScala.toList), (2, List("c", "d")))
  }

  test("skips empty YAML values") {
    assert(!YamlConfigFormat().parse(resource("formats/sample.yaml")).hasPath("app.empty"))
  }

  test("orders formats by precedence") {
    assertEquals(ConfigFormat.byPrecedence.flatMap(_.extensions), List("properties", "yaml", "yml", "conf"))
  }
}
