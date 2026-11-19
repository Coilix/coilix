package dev.coilix.concurrency
package custom

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Primary

@Configuration
final class CustomExecutorConfiguration {
  @Bean
  @Primary
  def callerRunsExecutor: TaskExecutor = CallerRunsExecutor()
}
