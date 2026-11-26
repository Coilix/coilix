package dev.coilix.config

import dev.coilix.config.ConfigError.orThrow
import java.time.Duration
import munit.FunSuite

final class TypesafeConfigTreeTest extends FunSuite {
  private val tree = ConfigTrees.parse(
    """app {
      |  name = demo
      |  port = 8080
      |  ratio = 0.5
      |  enabled = true
      |  timeout = 5s
      |  servers = [a, b]
      |  "dotted.key" = 1
      |}""".stripMargin
  )

  test("checks whether a path exists") {
    assert(tree.contains("app.name"))
    assert(!tree.contains("app.missing"))
  }

  test("recognises sections") {
    assert(tree.isSection("app"))
    assert(!tree.isSection("app.name"))
  }

  test("lists the keys of a section") {
    assertEquals(tree.keys("app").orThrow, List("dotted.key", "enabled", "name", "port", "ratio", "servers", "timeout"))
  }

  test("exposes list elements as trees") {
    assertEquals(tree.elements("app.servers").orThrow.map(_.string(ConfigTree.elementKey)), List(Right("a"), Right("b")))
  }

  test("quotes keys when joining paths") {
    assertEquals(tree.int(tree.child("app", "dotted.key")), Right(1))
  }

  test("reads typed values") {
    assertEquals(tree.string("app.name"), Right("demo"))
    assertEquals(tree.int("app.port"), Right(8080))
    assertEquals(tree.long("app.port"), Right(8080L))
    assertEquals(tree.double("app.ratio"), Right(0.5))
    assertEquals(tree.boolean("app.enabled"), Right(true))
    assertEquals(tree.duration("app.timeout"), Right(Duration.ofSeconds(5)))
    assertEquals(tree.anyRef("app.port"), Right(Int.box(8080)))
  }

  test("returns wrong types as configuration errors") {
    assert(tree.int("app.name").left.exists(_.isInstanceOf[ConfigError.Unreadable]))
  }

  test("has no value at a malformed path") {
    assert(!tree.contains("app..name"))
  }

  test("falls back to given values") {
    val extended = tree.withFallback(Map("app.name" -> "other", "app.extra" -> "x"))
    assertEquals((extended.string("app.name"), extended.string("app.extra")), (Right("demo"), Right("x")))
  }

  test("is empty by default") {
    assert(!ConfigTree.empty.contains("app"))
  }
}
