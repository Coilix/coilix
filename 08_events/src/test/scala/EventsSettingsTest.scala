package dev.coilix.events

import com.typesafe.config.ConfigFactory
import dev.coilix.concurrency.RetryPolicy
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import munit.FunSuite
import scala.concurrent.duration.DurationInt

final class EventsSettingsTest extends FunSuite {

  test("uses the defaults when nothing is configured") {
    val settings = EventsSettings.from(Environment.empty)
    assertEquals(
      (settings.shutdownTimeout, settings.listener("mailer", "onPlaced")),
      (10.seconds, EventListenerSettings(RetryPolicy.none))
    )
  }

  test("reads the shutdown timeout and a partial retry policy of one listener") {
    val config   = """
      coilix.events {
        shutdown-timeout = 3s
        listeners.mailer.onPlaced.retry { max-attempts = 4, initial-delay = 200ms }
      }
    """
    val settings = EventsSettings.from(Environment.of(TypesafeConfigTree(ConfigFactory.parseString(config))))
    assertEquals(
      (settings.shutdownTimeout, settings.listener("mailer", "onPlaced"), settings.listener("mailer", "onCancelled")),
      (3.seconds, EventListenerSettings(RetryPolicy(maxAttempts = 4, initialDelay = 200.millis)), EventListenerSettings())
    )
  }
}
