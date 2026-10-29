package io.github.coilix.slf4s

import munit.FunSuite

final class Slf4sPluginTest extends FunSuite {
  private val compiler = SnippetCompiler()

  private val annotation = "class Slf4s extends scala.annotation.StaticAnnotation\n"

  test("annotated class gets a logger") {
    assertEquals(compiler.errors(s"$annotation@Slf4s class Annotated { def hello(): Unit = log.info(\"hello\") }"), Nil)
  }

  test("annotated object gets a logger") {
    assertEquals(compiler.errors(s"$annotation@Slf4s object Annotated { def hello(): Unit = log.info(\"hello\") }"), Nil)
  }

  test("logger has the slf4j logger type") {
    assertEquals(
      compiler.errors(
        s"""package a.b
$annotation@Slf4s class Annotated { def logger: org.slf4j.Logger = log }
"""
      ),
      Nil
    )
  }

  test("class without annotation gets no logger") {
    assert(compiler.errors(s"${annotation}class Plain { def hello(): Unit = log.info(\"hello\") }").nonEmpty)
  }

  test("explicitly declared log member is kept") {
    assertEquals(
      compiler.errors(s"$annotation@Slf4s class Custom { val log: String = \"custom\"; def size: Int = log.length }"),
      Nil
    )
  }
}
