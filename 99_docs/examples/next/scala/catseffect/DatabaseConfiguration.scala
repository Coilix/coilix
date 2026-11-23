package com.example
package catseffect

import cats.effect.IO
import cats.effect.Resource
import dev.coilix.annotation.Bean
import dev.coilix.annotation.Configuration
import dev.coilix.annotation.ShutdownTimeout

@Configuration
class DatabaseConfiguration {

  @Bean
  @ShutdownTimeout("${shop.database.close-timeout:1m}")
  def connections: Resource[IO, ConnectionPool] =
    Resource.make(IO.blocking(ConnectionPool("jdbc:postgresql://localhost/shop")))(pool => IO.blocking(pool.close()))

  @Bean
  def migrations(pool: ConnectionPool): IO[MigrationReport] =
    IO.blocking(MigrationReport(applied = if (pool.isOpen) 3 else 0))
}
