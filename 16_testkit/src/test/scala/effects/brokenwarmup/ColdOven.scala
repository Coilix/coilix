package dev.coilix.testkit
package effects
package brokenwarmup

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct

@Component
final class ColdOven {

  @PostConstruct
  def heat(): IO[Unit] = IO.raiseError(IllegalStateException("the oven stays cold"))
}
