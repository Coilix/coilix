package dev.coilix.diagnostics

import cats.effect.IO
import cats.syntax.all.*
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationInt

final class LoggingFindingReporterTest extends CatsEffectSuite {
  private val warning  = Finding(FindingSeverity.Warning, Finding.longBlocked, "1 thread is blocked", Nil)
  private val critical = Finding(FindingSeverity.Critical, Finding.deadlock, "2 threads are deadlocked", Nil)

  test("logs a Critical finding every round and a warning of one kind once per repeat interval") {
    val clock    = MutableClock(Samples.start)
    val reporter = LoggingFindingReporter(1.minute, 10, clock)
    List(0.seconds, 30.seconds, 30.seconds)
      .traverse(step => IO(clock.advance(step)) *> reporter.due(List(warning, critical)).map(_.map(_.kind)))
      .assertEquals(
        List(List(Finding.longBlocked, Finding.deadlock), List(Finding.deadlock), List(Finding.longBlocked, Finding.deadlock))
      )
  }

  test("logs a warning of a kind once when it comes twice in one round") {
    LoggingFindingReporter(1.minute, 10, MutableClock(Samples.start))
      .due(List(warning, warning.copy(message = "2 threads are blocked")))
      .map(_.map(_.message))
      .assertEquals(List("1 thread is blocked"))
  }

  test("the text has the threads with their lock and stack") {
    val thread = ThreadSnapshot(
      7,
      "worker-1",
      Thread.State.BLOCKED,
      daemon = false,
      List(StackTraceElement("com.example.Shop", "pay", "Shop.scala", 12)),
      Some("java.lang.Object@1"),
      Some("worker-2")
    )
    val text   = LoggingFindingReporter(1.minute, 10, MutableClock(Samples.start)).textOf(warning.copy(threads = List(thread)))
    assertEquals(
      text,
      "long-blocked: 1 thread is blocked\n    \"worker-1\" BLOCKED waiting for java.lang.Object@1 held by worker-2\n        at com.example.Shop.pay(Shop.scala:12)"
    )
  }
}
