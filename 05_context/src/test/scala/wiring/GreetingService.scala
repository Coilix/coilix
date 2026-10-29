package io.github.coilix.context.wiring

trait GreetingService {
  def greet(name: String): String

  def greetAll(name: String): List[String]

  def hasFarewell: Boolean
}
