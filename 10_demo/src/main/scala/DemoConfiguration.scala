package io.github.coilix.demo

import io.github.coilix.annotation.Bean
import io.github.coilix.annotation.Configuration
import io.github.coilix.annotation.Value

@Configuration
final class DemoConfiguration {

  @Bean
  def greeter(@Value("${demo.greeting:Hi}") greeting: String): Greeter = TemplateGreeter(greeting)
}
