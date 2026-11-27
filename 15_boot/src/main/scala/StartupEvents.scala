package dev.coilix.boot

import cats.effect.IO
import cats.syntax.all.*
import dev.coilix.annotation.Slf4s
import dev.coilix.common.TypeInfo
import dev.coilix.context.ConfigurableApplicationContext
import dev.coilix.events.ApplicationFailed
import dev.coilix.events.ApplicationStarted
import dev.coilix.events.ContextReady
import dev.coilix.events.EventPublisher

/**
 * Publishes the startup events of the framework around the runners of a started context: [[dev.coilix.events.ContextReady]]
 * before them, [[dev.coilix.events.ApplicationStarted]] after them, and [[dev.coilix.events.ApplicationFailed]] when either
 * event or a runner fails. The listeners of the events are called in `IO.blocking`.
 *
 * A [[dev.coilix.boot.CoilixIOApp]] and the test contexts of the testkit start the same way. A context without the events module,
 * disabled with `coilix.modules.events.enabled = false`, publishes nothing.
 */
@Slf4s
object StartupEvents {

  /**
   * Publishes [[dev.coilix.events.ContextReady]], runs `runners` and publishes [[dev.coilix.events.ApplicationStarted]].
   *
   * When a listener of these events or `runners` fails, the failure is published as [[dev.coilix.events.ApplicationFailed]] and
   * fails the returned `IO`; closing the context is left to its owner. A failure of a listener of `ApplicationFailed` is logged.
   */
  def around(context: ConfigurableApplicationContext)(runners: IO[Unit]): IO[Unit] =
    (publish(context, ContextReady(context)) *> runners *> publish(context, ApplicationStarted(context)))
      .onError(cause => failure(context, cause))

  private def failure(context: ConfigurableApplicationContext, cause: Throwable): IO[Unit] =
    publish(context, ApplicationFailed(context, cause)).handleErrorWith(listenerFailure =>
      logF.error("A listener failed on ApplicationFailed", listenerFailure)
    )

  private def publish[E: TypeInfo](context: ConfigurableApplicationContext, event: E): IO[Unit] =
    context.getBeanOption[EventPublisher].traverse_(_.publish(event))
}
