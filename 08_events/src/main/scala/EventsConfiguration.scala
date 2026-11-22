package dev.coilix.events

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.concurrency.TaskExecutor
import dev.coilix.concurrency.TaskScheduler
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
  def eventBus(executor: TaskExecutor, scheduler: TaskScheduler, settings: EventsSettings): DefaultEventBus =
    DefaultEventBus(executor, scheduler, settings)

  /** Publishes [[dev.coilix.events.ContextClosing]] when the context starts closing. */
  @Bean
  def contextClosingPublisher(publisher: EventPublisher, context: ApplicationContext): ContextClosingPublisher =
    ContextClosingPublisher(publisher, context)
}
