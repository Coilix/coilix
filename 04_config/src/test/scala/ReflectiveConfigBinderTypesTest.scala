package dev.coilix.config

import dev.coilix.config.enums.Mode
import dev.coilix.config.enums.Plan
import dev.coilix.config.enums.Shape
import dev.coilix.config.fixture.ServerSettings
import dev.coilix.config.valuetype.Coordinates
import dev.coilix.config.valuetype.Port
import dev.coilix.config.valuetype.Secret
import dev.coilix.config.valuetype.Tags
import munit.FunSuite

final class ReflectiveConfigBinderTypesTest extends FunSuite {
  private val binder = ConfigBinder.default

  private val serverTree = ConfigTrees.parse(
    """server {
      |  port = 8080
      |  mode = read-only
      |  api-key = s3cr3t
      |  modes = [Fast, Safe]
      |  tags = [blue, green]
      |}""".stripMargin
  )

  test("binds value types and enums inside a class") {
    assertEquals(
      binder.bind(serverTree, "server", classOf[ServerSettings]),
      ServerSettings(Port(8080), Mode.ReadOnly, Secret("s3cr3t"), List(Mode.Fast, Mode.Safe), Tags(List("blue", "green")), None)
    )
  }

  test("binds a single value type and a single enum") {
    assertEquals(binder.bind(serverTree, "server.port", classOf[Port]), Port(8080))
    assertEquals(binder.bind(ConfigTrees.parse("plan = FREE"), "plan", classOf[Plan]), Plan.Basic)
  }

  test("reports a value rejected by the value type with the path") {
    val error = intercept[ConfigurationException](binder.bind(ConfigTrees.parse("port = 0"), "port", classOf[Port]))
    assertEquals(
      error.getMessage,
      "Cannot bind configuration 'port' to dev.coilix.config.valuetype.Port: " +
        "Cannot create dev.coilix.config.valuetype.Port from '0': requirement failed: Port must be positive, but is 0"
    )
  }

  test("reports an inner value of the wrong type") {
    intercept[ConfigurationException](binder.bind(ConfigTrees.parse("port = eighty"), "port", classOf[Port]))
  }

  test("reports an unknown enum name with the path and the allowed names") {
    val error = intercept[ConfigurationException](binder.bind(ConfigTrees.parse("mode = slow"), "mode", classOf[Mode]))
    assertEquals(
      error.getMessage,
      "Cannot bind configuration 'mode' to dev.coilix.config.enums.Mode: " +
        "Unknown dev.coilix.config.enums.Mode 'slow', expected one of: Fast, Safe, read-only"
    )
  }

  test("reports an invalid value type or enum definition with the path") {
    val tree = ConfigTrees.parse("value = 1")
    assert(intercept[ConfigurationException](binder.bind(tree, "value", classOf[Coordinates])).getMessage.contains("'value'"))
    assert(intercept[ConfigurationException](binder.bind(tree, "value", classOf[Shape])).getMessage.contains("'value'"))
  }
}
