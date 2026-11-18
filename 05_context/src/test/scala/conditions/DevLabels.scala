package dev.coilix.context
package conditions

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Profile

@Configuration
@Profile(Array("dev"))
final class DevLabels {
  @Bean
  def label: Label = Label("dev")
}
