package dev.coilix.context
package wiring

trait GreetingService {
  def greet(name: String): String

  def greetAll(name: String): List[String]

  def hasFarewell: Boolean
}
