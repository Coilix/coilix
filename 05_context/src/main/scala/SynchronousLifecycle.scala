package dev.coilix.context

import cats.effect.unsafe.IORuntime
import scala.util.control.NonFatal

/**
 * Starts a context outside `IO`, for code that has no fiber to run [[dev.coilix.context.ConfigurableApplicationContext.start]]
 * on, such as [[dev.coilix.context.ApplicationContextBuilder.build]] and the synchronous suites of the testkit. An application
 * started by `CoilixIOApp` does not use it.
 */
object SynchronousLifecycle {

  /**
   * Runs [[dev.coilix.context.ConfigurableApplicationContext.start]] of `context` and blocks until it completes, on the
   * `IORuntime` bean of the context or, when the context has none, on the global runtime of cats-effect. A failure closes the
   * context and is rethrown.
   *
   * @return
   *   the same context, started
   */
  def started(context: ConfigurableApplicationContext): ConfigurableApplicationContext =
    try {
      context.start.unsafeRunSync()(using runtimeOf(context))
      context
    } catch {
      case NonFatal(failure) =>
        context.close()
        throw failure
    }

  /** The `IORuntime` bean of `context`, or the global runtime of cats-effect when the context has none. */
  def runtimeOf(context: ApplicationContext): IORuntime =
    context.getBeanOption[IORuntime].getOrElse(IORuntime.global)
}
