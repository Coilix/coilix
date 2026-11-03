package dev.coilix.context.constructor

final class WidestConstructorBean(val first: String, val second: String) {
  def this(first: String) = this(first, first)
}
