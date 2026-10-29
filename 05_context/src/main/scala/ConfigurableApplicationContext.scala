package io.github.coilix.context

trait ConfigurableApplicationContext extends ApplicationContext with AutoCloseable {

  def isActive: Boolean

  override def close(): Unit
}
