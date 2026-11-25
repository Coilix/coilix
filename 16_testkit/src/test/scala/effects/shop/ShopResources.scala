package dev.coilix.testkit
package effects
package shop

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class ShopResources {

  @Bean
  def connections(journal: OpeningJournal): Resource[IO, ConnectionPool] =
    opened(journal, "pool", ConnectionPool("main"))

  @Bean
  def mailbox(journal: OpeningJournal): Resource[IO, Mailbox] =
    opened(journal, "mailbox", Mailbox("shop@example.com"))

  @Bean
  def catalog(pool: ConnectionPool, journal: OpeningJournal): Resource[IO, Catalog] =
    opened(journal, "catalog", Catalog(pool))

  @Bean
  def greeting(journal: OpeningJournal): IO[Greeting] =
    IO(journal.record("greeting loaded")).as(Greeting("Welcome"))

  private def opened[A](journal: OpeningJournal, name: String, value: A): Resource[IO, A] =
    Resource.make(IO(journal.record(s"$name opened")).as(value))(_ => IO(journal.record(s"$name closed")))
}
