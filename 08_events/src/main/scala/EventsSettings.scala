package dev.coilix.events

import dev.coilix.config.Environment
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * Settings of the events module, read from `coilix.events`. Every key is optional.
 *
 * ```yaml
 * coilix.events:
 *   shutdown-timeout: 20s
 *   synchronous: false
 *   listeners:
 *     confirmationMailer:
 *       onOrderPlaced:
 *         retry:
 *           max-attempts: 3
 *           initial-delay: 500ms
 * ```
 *
 * @param shutdownTimeout
 *   how long closing the context waits for running asynchronous deliveries before interrupting them
 * @param synchronous
 *   whether asynchronous listeners also run in the thread of the publisher, which tests use to check the effects of events
 *   without waiting; their exceptions are still only logged, and their retries follow at once, without the delays of the
 *   policy
 * @param listeners
 *   settings of listeners by bean name and then by method name
 */
final case class EventsSettings(
  shutdownTimeout: FiniteDuration                                  = 10.seconds,
  synchronous:     Boolean                                         = false,
  listeners:       Map[String, Map[String, EventListenerSettings]] = Map.empty
) {

  /** The settings of the listener method `method` of bean `bean`, or the defaults when it is not configured. */
  def listener(bean: String, method: String): EventListenerSettings =
    listeners.get(bean).flatMap(_.get(method)).getOrElse(EventListenerSettings())
}

/** Reading of [[dev.coilix.events.EventsSettings]] from the configuration. */
object EventsSettings {

  /** The configuration path of the settings. */
  val path: String = "coilix.events"

  /**
   * The settings under `coilix.events`, with defaults for missing keys.
   *
   * @throws dev.coilix.config.ConfigurationException
   *   if a value cannot be read, such as a duration without a unit
   */
  def from(environment: Environment): EventsSettings =
    environment.get[EventsSettings](path).getOrElse(EventsSettings())
}
