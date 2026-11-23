package dev.coilix.context
package dependson

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.DependsOn

@Configuration
final class WarmupConfiguration {
  @Bean
  @DependsOn(Array("priceCache", "schemaMigrations", "priceCache"))
  def cacheWarmer(journal: StartupJournal): CacheWarmer = {
    journal.record("cache warmed")
    CacheWarmer(journal)
  }
}
