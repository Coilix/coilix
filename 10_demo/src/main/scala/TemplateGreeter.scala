package dev.coilix.demo

/**
 * Greets with a fixed greeting, as in `Hello, Ann`.
 *
 * @param greeting
 *   the word that starts each greeting
 */
final class TemplateGreeter(greeting: String) extends Greeter {
  override def greet(name: String): String = s"$greeting, $name"
}
