package org.scaling.context.constructor

import org.scaling.annotation.Autowired

final class AutowiredConstructorBean(val first: String, val second: String) {
  @Autowired
  def this(first: String) = this(first, first)
}
