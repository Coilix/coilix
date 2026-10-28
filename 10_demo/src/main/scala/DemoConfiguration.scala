package org.scaling.demo

import org.scaling.annotation.Bean
import org.scaling.annotation.Configuration
import org.scaling.annotation.Value

@Configuration
final class DemoConfiguration {

  @Bean
  def greeter(@Value("${demo.greeting:Hi}") greeting: String): Greeter = TemplateGreeter(greeting)
}
