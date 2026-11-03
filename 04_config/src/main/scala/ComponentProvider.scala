package dev.coilix.config

trait ComponentProvider {
  def environment: Environment

  def get[T: ConfigPath](default: => T): T
}

object ComponentProvider {
  def from(environment: Environment): ComponentProvider = ConfigComponentProvider(environment)

  def builtIn: ComponentProvider = from(Environment.empty)
}
