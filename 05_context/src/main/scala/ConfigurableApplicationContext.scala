package dev.coilix.context

/**
 * An [[dev.coilix.context.ApplicationContext]] that the application can close.
 *
 * `Coilix.run` returns it and registers a shutdown hook that closes it when the JVM stops.
 */
trait ConfigurableApplicationContext extends ApplicationContext with AutoCloseable {

  /** Whether the context has not been closed yet. */
  def isActive: Boolean

  /** Closes the context. Closing an already closed context does nothing. */
  override def close(): Unit
}
