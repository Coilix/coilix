package dev.coilix.testkit
package effects
package slow

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.ShutdownTimeout
import java.util.concurrent.atomic.AtomicBoolean

@Configuration
final class DrainConfiguration {
  private val closed = AtomicBoolean(false)

  def isQuickDrainClosed: Boolean = closed.get

  @Bean
  @ShutdownTimeout("${test.drain-timeout:100ms}")
  def stuckDrain: Resource[IO, Drain] =
    Resource.make(IO.pure(Drain("stuck")))(_ => IO.never)

  @Bean
  def brokenDrain: Resource[IO, Drain] =
    Resource.make(IO.pure(Drain("broken")))(_ => IO.raiseError(IllegalStateException("the drain is broken")))

  @Bean
  def quickDrain: Resource[IO, Drain] =
    Resource.make(IO.pure(Drain("quick")))(_ => IO(closed.set(true)))
}
