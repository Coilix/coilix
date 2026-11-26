package dev.coilix.logging

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import munit.FunSuite

final class OutputTest extends FunSuite {

  private val event = LogEvent(
    Instant.parse("2026-11-26T14:03:22.123Z"),
    "io-compute-3",
    LogLevel.Warn,
    "dev.coilix.demo.workshop.FaultCodeExport",
    "Shard fault-code-export#0 is slow",
    None,
    Map("shard" -> "0", "job" -> "export")
  )

  test("formats the time, the level, the thread, the short logger name, the message and the MDC") {
    assertEquals(
      LogFormat(colors = false, ZoneOffset.UTC).format(event),
      "2026-11-26 14:03:22.123 WARN  [io-compute-3] d.c.d.w.FaultCodeExport - Shard fault-code-export#0 is slow {job=export, shard=0}"
    )
  }

  test("colors the time and the level and appends the stack trace of the exception") {
    val formatted = LogFormat(colors = true, ZoneOffset.UTC).format(event.copy(error = Some(IllegalStateException("late"))))
    assert(formatted.contains("\u001b[1;33mWARN \u001b[0m"), formatted)
    assert(formatted.contains("java.lang.IllegalStateException: late"), formatted)
  }

  test("the console appender writes one entry per event") {
    val bytes = ByteArrayOutputStream()
    ConsoleAppender(LogFormat(colors = false, ZoneOffset.UTC), PrintStream(bytes, true, StandardCharsets.UTF_8)).append(event)
    assertEquals(bytes.toString(StandardCharsets.UTF_8).linesIterator.size, 1)
  }

  test("the async appender writes every event on its thread and drains them when it closes") {
    val recording = RecordingAppender()
    val async     = AsyncAppender(recording, 2)
    List.range(0, 50).foreach(index => async.append(event.copy(message = s"m$index")))
    async.close()
    assertEquals(recording.recorded.map(_.message).sorted, List.range(0, 50).map(index => s"m$index").sorted.toVector)
  }
}
