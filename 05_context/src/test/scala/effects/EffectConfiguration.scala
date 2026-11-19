package dev.coilix.context
package effects

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class EffectConfiguration {
  @Bean
  def report: Later[Report] = Later(Report("daily"))

  @Bean
  def archive: Scoped[Option, Archive] = Scoped(Archive("monthly"))
}
