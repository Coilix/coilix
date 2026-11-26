package dev.coilix.context

import cats.effect.IO

/**
 * An [[dev.coilix.context.ApplicationContext]] that its owner starts and closes.
 *
 * A `CoilixIOApp` creates it, runs [[dev.coilix.context.ConfigurableApplicationContext.start]] and runs
 * [[dev.coilix.context.ConfigurableApplicationContext.shutdown]] when the application ends, also after a `SIGTERM`. Code
 * outside `IO`, such as a synchronous test, gets a started context from
 * [[dev.coilix.context.ApplicationContextBuilder.build]] and closes it with `close()`.
 */
trait ConfigurableApplicationContext extends ApplicationContext with AutoCloseable {

  /** Whether the context has not been closed yet. */
  def isActive: Boolean

  /**
   * Runs [[dev.coilix.context.ContextLifecycleListener.onContextStarted]] of the listener beans in creation order. The owner of
   * the context runs it once, after the beans are created; a failure leaves closing the context to the owner.
   */
  def start: IO[Unit]

  /**
   * Closes the context: runs [[dev.coilix.context.ContextLifecycleListener.onContextClosing]] of the listener beans phase by
   * phase in the order of [[dev.coilix.context.ClosingPhase]], then the `@PreDestroy` methods of the beans, starting with the
   * bean created last. Shutting down an already closed context does nothing.
   */
  def shutdown: IO[Unit]

  /**
   * Closes the context like [[dev.coilix.context.ConfigurableApplicationContext.shutdown]], blocking the calling thread until the
   * beans are destroyed. Closing an already closed context does nothing.
   */
  override def close(): Unit
}
