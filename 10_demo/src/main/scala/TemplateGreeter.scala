package org.scaling.demo

final class TemplateGreeter(greeting: String) extends Greeter {
  override def greet(name: String): String = s"$greeting, $name"
}
