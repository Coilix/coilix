package dev.coilix.context.constructor

import dev.coilix.annotation.Autowired

final class AutowiredConstructorBean(val first: String, val second: String) {
  @Autowired
  def this(first: String) = this(first, first)
}
