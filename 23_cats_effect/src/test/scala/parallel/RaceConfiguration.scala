package dev.coilix.catseffect
package parallel

import cats.effect.IO
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class RaceConfiguration {

  @Bean
  def left(line: StartingLine): IO[Runner] = IO.blocking(line.await()).as(Runner("left"))

  @Bean
  def right(line: StartingLine): IO[Runner] = IO.blocking(line.await()).as(Runner("right"))
}
