package com.example
package replicas

import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup

final class InvoiceBatchesTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[ReplicasApplication.type]

  test("a single replica takes every customer") {
    setup.use {
      assertEquals(bean[InvoiceBatches].mine(List(1, 2, 3)), List(1, 2, 3))
    }
  }

  test("the second of three replicas takes its share") {
    setup
      .withProperties(
        "coilix.cluster.identity"      -> "static",
        "coilix.cluster.replicas"      -> "3",
        "coilix.cluster.replica-index" -> "1"
      )
      .use {
        assertEquals(bean[InvoiceBatches].mine(List(1, 2, 3, 4, 5, 6, 7)), List(1, 4, 7))
      }
  }
}
