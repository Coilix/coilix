package io.github.coilix.context.constructor

import io.github.coilix.annotation.Autowired

final class AutowiredConstructorBean(val first: String, val second: String) {
  @Autowired
  def this(first: String) = this(first, first)
}
