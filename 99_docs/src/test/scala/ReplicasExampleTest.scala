package com.example

import dev.coilix.cluster.ClusterSettings
import dev.coilix.cluster.IdentityMode
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import java.nio.file.Paths

class ReplicasExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration selects the stateful-set identity") {
    val url = resources.resolve("guides/replicas/application.yaml").toUri.toURL
    assertEquals(
      ClusterSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url)))),
      ClusterSettings(IdentityMode.StatefulSet, replicas = Some(3), version = "1.4.0")
    )
  }
}
