package dev.coilix.context
package qualified

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Qualifier
import dev.coilix.context.wiring.Greeter

@Configuration
final class BannerConfiguration {
  @Bean
  def banner(@Qualifier("englishGreeter") greeter: Greeter): Banner = Banner(greeter.greet("guests"))
}
