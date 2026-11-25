package dev.coilix.boot
package tagless

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

/** Registers the type class instance of the effect. */
@Configuration
final class TaskConfiguration {

  /** The `Delay` instance of `Task`. */
  @Bean
  def taskDelay(): Delay[Task] = new Delay[Task] {}
}
