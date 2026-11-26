package com.example

import dev.coilix.concurrency.RetryPolicy
import dev.coilix.config.ConfigError.orThrow
import dev.coilix.config.Environment
import dev.coilix.config.TypesafeConfigTree
import dev.coilix.config.YamlConfigFormat
import dev.coilix.events.EventListenerSettings
import dev.coilix.events.EventsSettings
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class EventsExampleTest extends munit.FunSuite {

  private val resources = Paths.get(sys.props.getOrElse("coilix.docs.resources", "99_docs/examples/next/resources"))

  test("the guide configuration sets the shutdown timeout and the retries of the mailer") {
    val url      = resources.resolve("guides/events/application.yaml").toUri.toURL
    val settings = EventsSettings.from(Environment.of(TypesafeConfigTree(YamlConfigFormat().parse(url).orThrow)))
    assertEquals(
      (settings.shutdownTimeout, settings.listener("confirmationMailer", "onOrderPlaced")),
      (20.seconds, EventListenerSettings(RetryPolicy(maxAttempts = 3, initialDelay = 500.millis)))
    )
  }
}
