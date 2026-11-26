package dev.coilix.context

import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.IORuntime

/**
 * The only place where Coilix runs `IO` outside a fiber: for code that has no fiber to run it on, such as
 * [[dev.coilix.context.ApplicationContextBuilder.build]], `close()` of a context and the synchronous suites of the testkit. An
 * application started by `CoilixIOApp` does not use it.
 */
object SynchronousLifecycle {

  /**
   * Opens `context` on the global runtime of cats-effect, runs [[dev.coilix.context.ConfigurableApplicationContext.start]] and
   * blocks until both complete. A failing start shuts the context down and is rethrown.
   *
   * @return
   *   the context, started; `close()` releases it
   */
  def started(context: Resource[IO, ConfigurableApplicationContext]): ConfigurableApplicationContext =
    opened(context.evalTap(_.start))

  /**
   * Opens `context` on the global runtime of cats-effect and blocks until it is open. The release of the resource is dropped, so
   * it must do no more than [[dev.coilix.context.ConfigurableApplicationContext.shutdown]], which `close()` of the context does.
   * A failure of the resource is rethrown after the resource has released what it opened.
   */
  def opened(context: Resource[IO, ConfigurableApplicationContext]): ConfigurableApplicationContext =
    context.allocated.map(_._1).unsafeRunSync()(using IORuntime.global)

  /** Runs `effect` on the runtime of `context` and blocks until it completes, rethrowing its failure. */
  def run[A](context: ApplicationContext, effect: IO[A]): A =
    effect.unsafeRunSync()(using runtimeOf(context))

  /** The `IORuntime` bean of `context`, or the global runtime of cats-effect when the context has none. */
  def runtimeOf(context: ApplicationContext): IORuntime =
    context.getBeanOption[IORuntime].getOrElse(IORuntime.global)
}
