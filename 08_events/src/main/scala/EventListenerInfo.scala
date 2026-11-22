package dev.coilix.events

import dev.coilix.common.GenericType
import dev.coilix.concurrency.RetryPolicy

/**
 * Describes a listener found by the [[dev.coilix.events.EventBus]].
 *
 * @param name
 *   `<bean>.<method>`, used in log messages and in the `coilix.events.listeners` configuration
 * @param eventType
 *   the type of the events the listener receives, together with their subtypes
 * @param async
 *   whether the listener runs on the task executor
 * @param retry
 *   how an asynchronous listener retries a failed delivery
 */
final case class EventListenerInfo(name: String, eventType: GenericType, async: Boolean, retry: RetryPolicy)
