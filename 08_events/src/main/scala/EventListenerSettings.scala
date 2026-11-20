package dev.coilix.events

import dev.coilix.concurrency.RetryPolicy

/**
 * Settings of one listener, read from `coilix.events.listeners.<bean>.<method>`.
 *
 * @param retry
 *   how an asynchronous listener retries a failed delivery, by default a single try
 */
final case class EventListenerSettings(retry: RetryPolicy = RetryPolicy.none)
