package dev.coilix.catseffect
package ioapp

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration

@Configuration
final class TillConfiguration {

  @Bean
  def drawer: Resource[IO, Drawer] =
    Resource.make(IO(IOAppJournal.record("drawer opened")).as(Drawer(100)))(_ => IO(IOAppJournal.record("drawer closed")))

  @Bean
  def receipts(drawer: Drawer): Resource[IO, Receipts] =
    Resource.make(IO(IOAppJournal.record("receipts opened")).as(Receipts(drawer.cash)))(_ =>
      IO(IOAppJournal.record("receipts closed"))
    )
}
