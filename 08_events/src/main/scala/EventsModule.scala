package dev.coilix.events

import dev.coilix.context.CoilixModule

/**
 * Adds the [[dev.coilix.events.EventBus]] to every application, which delivers published events to the methods annotated with
 * [[dev.coilix.annotation.EventListener]] and to the [[dev.coilix.events.ApplicationEventListener]] beans.
 *
 * The module is part of the core and is on the classpath of `coilix-boot`. It can be disabled with
 * `coilix.modules.events.enabled = false`.
 */
final class EventsModule extends CoilixModule {

  override def name: String = "events"

  override def components: List[Class[?]] = List(classOf[EventsConfiguration])
}
