package dev.coilix.boot

import dev.coilix.annotation.Slf4s
import dev.coilix.common.TypeInfo
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.events.ApplicationFailed
import dev.coilix.events.ApplicationStarted
import dev.coilix.events.ContextReady
import dev.coilix.events.EventPublisher
import scala.util.control.NonFatal

/**
 * Publishes the startup events of the framework around the runners of a built context: [[dev.coilix.events.ContextReady]]
 * before them, [[dev.coilix.events.ApplicationStarted]] after them, and [[dev.coilix.events.ApplicationFailed]] when either
 * event or a runner fails.
 *
 * `Coilix.run` and the test contexts of the testkit start the same way. A context without the events module, disabled with
 * `coilix.modules.events.enabled = false`, publishes nothing.
 */
@Slf4s
object StartupEvents {

  /**
   * Publishes [[dev.coilix.events.ContextReady]], runs `runners` and publishes [[dev.coilix.events.ApplicationStarted]].
   *
   * When a listener of these events or `runners` throws, the exception is published as [[dev.coilix.events.ApplicationFailed]],
   * the context is closed and the exception is rethrown. An exception of a listener of `ApplicationFailed` is logged.
   *
   * @return
   *   `context`, started
   */
  def around(context: ConfigurableApplicationContext)(runners: => Unit): ConfigurableApplicationContext =
    try {
      publish(context, ContextReady(context))
      runners
      publish(context, ApplicationStarted(context))
      context
    } catch {
      case NonFatal(failure) => failed(context, failure)
    }

  private def failed(context: ConfigurableApplicationContext, failure: Throwable): Nothing = {
    try {
      publish(context, ApplicationFailed(context, failure))
    } catch {
      case NonFatal(listenerFailure) => log.error("A listener failed on ApplicationFailed", listenerFailure)
    }
    context.close()
    throw failure
  }

  private def publish[E: TypeInfo](context: ConfigurableApplicationContext, event: E): Unit =
    context.getBeanOption[EventPublisher].foreach(_.publish(event))
}
