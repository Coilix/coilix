package dev.coilix.context

/**
 * An [[dev.coilix.context.ApplicationContext]] that the application can close.
 *
 * A `CoilixIOApp` builds it and closes it when the application ends, also after a `SIGTERM`.
 */
trait ConfigurableApplicationContext extends ApplicationContext with AutoCloseable {

  /** Whether the context has not been closed yet. */
  def isActive: Boolean

  /**
   * Closes the context and runs the `@PreDestroy` methods of its beans, starting with the bean created last. Closing an already
   * closed context does nothing.
   */
  override def close(): Unit
}
