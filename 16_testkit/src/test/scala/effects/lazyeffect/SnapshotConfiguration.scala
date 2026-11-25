package dev.coilix.testkit
package effects
package lazyeffect

import cats.effect.IO
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.Lazy

@Configuration
final class SnapshotConfiguration {

  @Bean
  @Lazy
  def snapshot: IO[Snapshot] = IO.pure(Snapshot(true))
}
