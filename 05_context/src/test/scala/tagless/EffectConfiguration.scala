package dev.coilix.context
package tagless

import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

/** Registers the `Async` instance of the effect and a bean made by a generic `@Bean` method. */
@Configuration
final class EffectConfiguration {

  /** The type class instance of the effect. */
  @Bean
  def effectAsync(): Async[Effect] = new Async[Effect] {}

  /** A repository made by a `@Bean` method in tagless final style. */
  @Bean
  def orders[F[_]](sync: Sync[F]): OrderRepository[F] = InMemoryOrders(sync)
}
