package io.github.coilix.config

import munit.FunSuite
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

final class DefaultScalarReaderTest extends FunSuite {
  private val reader = DefaultScalarReader()
  private val tree   = ConfigTrees.parse("number = 7, flag = \"true\", timeout = 2 minutes, text = abc")

  test("supports primitive and boxed types") {
    assert(List(classOf[Int], classOf[java.lang.Integer], classOf[Boolean], classOf[String]).forall(reader.supports))
  }

  test("does not support other classes") {
    assert(!reader.supports(classOf[Thread]))
  }

  test("converts values to the requested type") {
    assertEquals(reader.read(tree, "number", classOf[Int]), Int.box(7))
    assertEquals(reader.read(tree, "number", classOf[Long]), Long.box(7L))
    assertEquals(reader.read(tree, "number", classOf[String]), "7")
    assertEquals(reader.read(tree, "flag", classOf[Boolean]), Boolean.box(true))
  }

  test("reads durations") {
    assertEquals(reader.read(tree, "timeout", classOf[FiniteDuration]), 2.minutes)
    assertEquals(reader.read(tree, "timeout", classOf[java.time.Duration]), java.time.Duration.ofMinutes(2))
  }

  test("fails for an unsupported type") {
    intercept[ConfigurationException](reader.read(tree, "text", classOf[Thread]))
  }
}
