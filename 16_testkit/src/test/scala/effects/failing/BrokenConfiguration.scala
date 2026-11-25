package dev.coilix.testkit
package effects
package failing

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class BrokenConfiguration {

  @Bean
  def ledger(journal: FailureJournal): Resource[IO, Ledger] =
    Resource.make(IO(journal.record("ledger opened")).as(Ledger("main")))(_ => IO(journal.record("ledger closed")))

  @Bean
  def forecast(ledger: Ledger): IO[Forecast] =
    IO.raiseError(IllegalStateException("the forecast service is down"))
}
