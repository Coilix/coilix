package dev.coilix.logging

import java.util.concurrent.atomic.AtomicReference
import munit.FunSuite
import org.slf4j.MDC

final class CoilixLoggerFactoryTest extends FunSuite {

  private def factoryWith(settings: LoggingSettings, appender: RecordingAppender): CoilixLoggerFactory =
    CoilixLoggerFactory(AtomicReference(settings), AtomicReference(Map.empty), AtomicReference(appender))

  test("gives every logger the level of its package and the same logger for the same name") {
    val recording = RecordingAppender()
    val factory   = factoryWith(LoggingSettings(levels = Map("com.example.noisy" -> LogLevel.Off)), recording)
    factory.getLogger("com.example.noisy.Chatter").error("hidden")
    factory.getLogger("com.example.Shop").info("Order {} paid with {}", 7, "card")
    factory.getLogger("com.example.Shop").debug("dropped")
    assertEquals(
      (recording.messages, factory.getLogger("com.example.Shop") eq factory.getLogger("com.example.Shop")),
      (Vector("INFO com.example.Shop Order 7 paid with card"), true)
    )
  }

  test("changes the levels of existing loggers at runtime and resets them") {
    val recording = RecordingAppender()
    val factory   = factoryWith(LoggingSettings(), recording)
    val billing   = factory.getLogger("com.example.billing.Invoices")
    billing.debug("before")
    factory.setLevel("com.example.billing", Some(LogLevel.Debug))
    billing.debug("during")
    factory.setLevel("com.example.billing", None)
    billing.debug("after")
    factory.setRootLevel(LogLevel.Error)
    billing.warn("quiet")
    assertEquals(recording.messages, Vector("DEBUG com.example.billing.Invoices during"))
  }

  test("configure applies new levels to loggers created before") {
    val recording = RecordingAppender()
    val factory   = factoryWith(LoggingSettings(), recording)
    val consumer  = factory.getLogger("org.apache.kafka.Consumer")
    factory.configure(LoggingSettings(levels = Map("org.apache.kafka" -> LogLevel.Warn)))
    consumer.info("hidden")
    consumer.warn("shown")
    assertEquals(recording.messages, Vector("WARN org.apache.kafka.Consumer shown"))
  }

  test("passes the exception and the MDC of the thread") {
    val recording = RecordingAppender()
    val logger    = factoryWith(LoggingSettings(), recording).getLogger("com.example.Shop")
    MDC.put("order", "7")
    try {
      logger.error("Payment failed", IllegalStateException("declined"))
    } finally {
      MDC.remove("order")
    }
    val event     = recording.recorded.head
    assertEquals((event.error.map(_.getMessage), event.context), (Some("declined"), Map("order" -> "7")))
  }
}
