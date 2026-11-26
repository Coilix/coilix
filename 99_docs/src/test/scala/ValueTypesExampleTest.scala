package com.example

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ConfigError.orThrow
import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import dev.coilix.config.valuetype.Secret
import java.nio.file.Paths

class ValueTypesExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  private def environment(yaml: String): Environment =
    Environment.of(TypesafeConfigTree(ConfigFactory.parseString(yaml).resolve()))

  test("the guide configuration binds to ShopSettings") {
    val url      = resources.resolve("guides/value-types/application.yaml").toUri.toURL
    val settings = Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url).orThrow)).bind("shop", classOf[ShopSettings])
    assertEquals(
      settings,
      ShopSettings(Port(8080), ShopMode.ReadOnly, Secret("local-test-key"), List(Port(8081), Port(8082)))
    )
    assertEquals(settings.toString, "ShopSettings(Port(8080),ReadOnly,*****,List(Port(8081), Port(8082)))")
  }

  test("an alias and another letter case find the same mode") {
    assertEquals(environment("mode = BROWSE-ONLY").bind("mode", classOf[ShopMode]), ShopMode.ReadOnly)
  }

  test("a rejected port and an unknown mode stop with the path") {
    val port = intercept[ConfigurationException](environment("shop.port = 0").bind("shop.port", classOf[Port]))
    assert(port.getMessage.contains("'shop.port'"), port.getMessage)
    val mode = intercept[ConfigurationException](environment("mode = closing").bind("mode", classOf[ShopMode]))
    assert(mode.getMessage.contains("expected one of: Open, Closed, read-only"), mode.getMessage)
  }
}
