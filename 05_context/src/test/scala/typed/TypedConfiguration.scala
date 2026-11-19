package dev.coilix.context
package typed

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class TypedConfiguration {
  @Bean
  def vectorRepo: Repo[Vector] = GenericRepo[Vector]("vector")
}
