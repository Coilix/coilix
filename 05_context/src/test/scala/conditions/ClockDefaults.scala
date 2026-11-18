package dev.coilix.context
package conditions

import dev.coilix.annotation.Bean
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.Configuration

@Configuration
final class ClockDefaults {
  @Bean
  @ConditionalOnMissingBean
  def clock: Clock = SystemClock()
}
