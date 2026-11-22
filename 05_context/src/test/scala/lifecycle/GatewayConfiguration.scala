package dev.coilix.context
package lifecycle

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class GatewayConfiguration {
  @Bean
  def gateway(journal: Journal): Gateway = HttpGateway(journal)
}
