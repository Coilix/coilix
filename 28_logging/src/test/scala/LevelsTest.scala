package dev.coilix.logging

import munit.FunSuite

final class LevelsTest extends FunSuite {

  test("parses level names ignoring case and spaces") {
    assertEquals(
      List("warn", "OFF", " Debug ", "loud").map(LogLevel.parse),
      List(Some(LogLevel.Warn), Some(LogLevel.Off), Some(LogLevel.Debug), None)
    )
  }

  test("a level enables messages at it and above, and off enables nothing") {
    assertEquals(
      (LogLevel.Info.enables(LogLevel.Warn), LogLevel.Info.enables(LogLevel.Debug), LogLevel.Off.enables(LogLevel.Error)),
      (true, false, false)
    )
  }

  test("the longest matching package wins and other loggers use the root level") {
    val resolver = LevelResolver(
      LogLevel.Info,
      Map("org.apache" -> LogLevel.Warn, "org.apache.kafka" -> LogLevel.Off, "com.example.Billing" -> LogLevel.Debug)
    )
    assertEquals(
      List(
        "org.apache.kafka.clients.Consumer",
        "org.apache.http.Client",
        "org.apachex.Other",
        "com.example.Billing",
        "com.example.Billing$Invoice",
        "com.example.Shop"
      ).map(resolver.levelOf),
      List(LogLevel.Off, LogLevel.Warn, LogLevel.Info, LogLevel.Debug, LogLevel.Debug, LogLevel.Info)
    )
  }

  test("shortens packages to their first letter and drops the dollar of objects") {
    assertEquals(
      List("dev.coilix.demo.workshop.FaultCodeExport", "Shop$", "ROOT").map(LoggerNames.shortened),
      List("d.c.d.w.FaultCodeExport", "Shop", "ROOT")
    )
  }
}
