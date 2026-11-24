package dev.coilix.context
package failing

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

/** A configuration whose `@Bean` method throws. */
@Configuration
final class BrokenConfiguration {

  @Bean
  def weight(): String = throw IllegalArgumentException("no weight")
}
