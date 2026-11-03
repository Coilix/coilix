package dev.coilix.demo

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Value

@Configuration
final class DemoConfiguration {

  @Bean
  def greeter(@Value("${demo.greeting:Hi}") greeting: String): Greeter = TemplateGreeter(greeting)
}
