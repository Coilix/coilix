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
 * A [[dev.coilix.boot.CoilixIOApp]] and the test contexts of the testkit start the same way. A context without the events module, disabled with
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
      ready(context)
      runners
      started(context)
      context
    } catch {
      case NonFatal(failure) => failed(context, failure)
    }

  /** Publishes [[dev.coilix.events.ContextReady]] for a start that runs the runners itself, rethrowing a failing listener. */
  def ready(context: ConfigurableApplicationContext): Unit =
    publish(context, ContextReady(context))

  /** Publishes [[dev.coilix.events.ApplicationStarted]] for a start that runs the runners itself, rethrowing a failing listener. */
  def started(context: ConfigurableApplicationContext): Unit =
    publish(context, ApplicationStarted(context))

  /**
   * Publishes [[dev.coilix.events.ApplicationFailed]] with `cause` for a start that runs the runners itself and closes the context
   * afterwards. A failure of a listener is logged.
   */
  def failure(context: ConfigurableApplicationContext, cause: Throwable): Unit =
    try {
      publish(context, ApplicationFailed(context, cause))
    } catch {
      case NonFatal(listenerFailure) => log.error("A listener failed on ApplicationFailed", listenerFailure)
    }

  private def failed(context: ConfigurableApplicationContext, cause: Throwable): Nothing = {
    failure(context, cause)
    context.close()
    throw cause
  }

  private def publish[E: TypeInfo](context: ConfigurableApplicationContext, event: E): Unit =
    context.getBeanOption[EventPublisher].foreach(_.publish(event))
}
