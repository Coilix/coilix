package dev.coilix.context.wiring

import dev.coilix.annotation.Service

@Service
final class DefaultGreetingService(greeter: Greeter, farewell: Option[Farewell], greeters: List[Greeter]) extends GreetingService {
  override def greet(name: String): String = greeter.greet(name)

  override def greetAll(name: String): List[String] = greeters.map(_.greet(name))

  override def hasFarewell: Boolean = farewell.isDefined
}
