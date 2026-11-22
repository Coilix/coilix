package dev.coilix.context
package conditions

import dev.coilix.annotation.Bean
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.Configuration

@Configuration
final class ConcreteClockDefaults {
  @Bean
  @ConditionalOnMissingBean(Array(classOf[Clock]))
  def clock: SystemClock = SystemClock()
}
