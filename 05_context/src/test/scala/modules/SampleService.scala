package dev.coilix.context
package modules

import dev.coilix.annotation.Component

@Component
final class SampleService {
  def greet: String = "hello from a module"
}
