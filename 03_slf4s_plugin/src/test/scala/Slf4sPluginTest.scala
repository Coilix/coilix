package dev.coilix.slf4s

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

  test("annotated class gets a logger in IO named logF when IOLogger is on the classpath") {
    assertEquals(
      SnippetCompiler(withEffectfulLogger = true).errors(
        s"$annotation@Slf4s class Annotated { def hello: cats.effect.IO[Unit] = logF.info(\"hello\") }"
      ),
      Nil
    )
  }

  test("annotated object gets a logger in IO named logF") {
    assertEquals(
      SnippetCompiler(withEffectfulLogger = true).errors(
        s"$annotation@Slf4s object Annotated { def hello: cats.effect.IO[Unit] = logF.warn(\"hello\") }"
      ),
      Nil
    )
  }

  test("no logF is added when IOLogger is not on the classpath") {
    assert(compiler.errors(s"$annotation@Slf4s class Annotated { def hello = logF }").nonEmpty)
  }

  test("explicitly declared logF member is kept") {
    assertEquals(
      SnippetCompiler(withEffectfulLogger = true).errors(
        s"$annotation@Slf4s class Custom { val logF: Int = 1; def size: Int = logF + 1 }"
      ),
      Nil
    )
  }
}
