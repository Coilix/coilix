package dev.coilix.boot
package modules

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class ReportConfiguration {
  @Bean
  def reportTitle: ReportTitle = ReportTitle("Monthly report")
}
