package dev.coilix.config

import dev.coilix.config.ConfigError.orThrow
import munit.FunSuite
import scala.jdk.CollectionConverters.*

final class ConfigFormatTest extends FunSuite {
  private def resource(name: String) = getClass.getClassLoader.getResource(name)

  private def parsed(format: ConfigFormat, name: String) = format.parse(resource(name)).orThrow

  test("parses HOCON") {
    val config = parsed(HoconConfigFormat(), "formats/sample.conf")
    assertEquals((config.getString("app.name"), config.getStringList("app.servers").asScala.toList), ("conf", List("a", "b")))
  }

  test("parses properties") {
    val config = parsed(PropertiesConfigFormat(), "formats/sample.properties")
    assertEquals((config.getString("app.name"), config.getStringList("app.servers").asScala.toList), ("props", List("e", "f")))
  }

  test("parses YAML") {
    val config = parsed(YamlConfigFormat(), "formats/sample.yaml")
    assertEquals((config.getInt("app.port"), config.getStringList("app.servers").asScala.toList), (2, List("c", "d")))
  }

  test("skips empty YAML values") {
    assert(!parsed(YamlConfigFormat(), "formats/sample.yaml").hasPath("app.empty"))
  }

  test("orders formats by precedence") {
    assertEquals(ConfigFormat.byPrecedence.flatMap(_.extensions), List("properties", "yaml", "yml", "conf"))
  }

  test("returns an error for a file that is not valid in its format") {
    val result = HoconConfigFormat().parse(resource("broken/syntax.conf"))
    assert(result.left.exists(_.isInstanceOf[ConfigError.Unparsable]), result)
  }
}
