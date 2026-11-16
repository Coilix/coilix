package com.example

import dev.coilix.annotation.Bean
import dev.coilix.annotation.ConditionalOnMissingBean
import dev.coilix.annotation.Configuration
import java.time.Clock

@Configuration
final class ClockConfiguration {
  @Bean
  @ConditionalOnMissingBean
  def clock: Clock = Clock.systemUTC()
}
