package dev.coilix.demo

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Value

/**
 * Declares the [[dev.coilix.demo.Greeter]] bean through a `@Bean` method, with the greeting read from `demo.greeting` and `Hi` as
 * the
 * default.
 */
@Configuration
final class DemoConfiguration {

  /** A greeter that uses the configured greeting. */
  @Bean
  def greeter(@Value("${demo.greeting:Hi}") greeting: String): Greeter = TemplateGreeter(greeting)
}
