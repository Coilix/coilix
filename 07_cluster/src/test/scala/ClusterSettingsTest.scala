package dev.coilix.cluster

import com.typesafe.config.ConfigFactory
import dev.coilix.config.ConfigurationException
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import munit.FunSuite

final class ClusterSettingsTest extends FunSuite {

  private def environment(hocon: String): Environment =
    Environment.of(TypesafeConfigTree(ConfigFactory.parseString(hocon)))

  test("uses the defaults when nothing is configured") {
    assertEquals(ClusterSettings.from(Environment.empty), ClusterSettings())
  }

  test("reads every key") {
    val settings = ClusterSettings.from(environment("""
      coilix.cluster { identity = stateful-set, replicas = 3, replica-index = 1, replica-id = shop-1, version = "2.0.0" }
    """))
    assertEquals(settings, ClusterSettings(IdentityMode.StatefulSet, Some(3), Some(1), Some("shop-1"), "2.0.0"))
  }

  test("an unknown identity mode stops with the path") {
    val error = intercept[ConfigurationException](ClusterSettings.from(environment("coilix.cluster.identity = dynamic")))
    assert(error.getMessage.contains("coilix.cluster.identity"), error.getMessage)
  }
}
