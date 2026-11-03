package dev.coilix.demo

/** Builds a greeting. The bean is declared in [[dev.coilix.demo.DemoConfiguration]]. */
trait Greeter {

  /** A greeting for `name`. */
  def greet(name: String): String
}
