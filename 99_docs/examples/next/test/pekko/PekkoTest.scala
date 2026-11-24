package com.example
package pekko

import dev.coilix.testkit.CoilixContextOps
import dev.coilix.testkit.CoilixTestSetup
import java.nio.file.Files
import org.apache.pekko.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class PekkoTest extends munit.FunSuite with CoilixContextOps {
  private val setup = CoilixTestSetup.forApplication[PekkoApplication.type]

  test("a bean runs a stream with the materializer of the application") {
    setup.use {
      assertEquals(Await.result(bean[OrderTotals].total(List(10, -5, 20)), 5.seconds), BigDecimal(30))
    }
  }

  test("a file sink of Pekko writes on the blocking executor") {
    val file = Files.createTempFile("report", ".txt")
    setup.use {
      val _ = Await.result(bean[ReportFiles].write(file, List("tea", "coffee")), 5.seconds)
    }
    assertEquals(Files.readString(file), "tea\ncoffee\n")
  }

  test("the ActorSystem stops when the application closes") {
    assert(setup.use(bean[ActorSystem]).whenTerminated.isCompleted)
  }
}
