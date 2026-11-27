package dev.coilix.events

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.config.Environment
import dev.coilix.context.ApplicationContext

/**
 * The beans of the events module: `eventsSettings`, `eventBus`, which publishes events and delivers them to listeners, and
 * `contextClosingPublisher`, which publishes [[dev.coilix.events.ContextClosing]].
 */
@Configuration
final class EventsConfiguration {

  /** The settings under `coilix.events`, or the defaults in a context without an environment. */
  @Bean
  def eventsSettings(environment: Option[Environment]): EventsSettings =
    environment.fold(EventsSettings())(EventsSettings.from)

  /** The in-memory event bus, injected as [[dev.coilix.events.EventPublisher]] or [[dev.coilix.events.EventBus]]. */
  @Bean
  def eventBus(settings: EventsSettings): Resource[IO, DefaultEventBus] =
    DefaultEventBus.resource(settings)

  /** Publishes [[dev.coilix.events.ContextClosing]] when the context starts closing. */
  @Bean
  def contextClosingPublisher(publisher: EventPublisher, context: ApplicationContext): ContextClosingPublisher =
    ContextClosingPublisher(publisher, context)
}
