package dev.coilix.catseffect
package lazywarmup

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Lazy
import dev.coilix.annotation.PostConstruct

@Component
@Lazy
final class LazyWarmup {

  @PostConstruct
  def warm(): IO[Unit] = IO.unit
}
