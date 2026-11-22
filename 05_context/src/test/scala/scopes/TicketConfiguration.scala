package dev.coilix.context
package scopes

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Prototype
import java.util.concurrent.atomic.AtomicInteger

@Configuration
final class TicketConfiguration {
  private val numbers = AtomicInteger()

  @Bean
  @Prototype
  def ticket(): Ticket = Ticket(numbers.incrementAndGet())
}
