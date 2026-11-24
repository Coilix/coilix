package com.example
package catseffect

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.PostConstruct
import dev.coilix.annotation.PreDestroy
import java.util.concurrent.atomic.AtomicReference

@Component
class ProductCache(report: MigrationReport) {
  private val products = AtomicReference(Map.empty[String, Int])

  def size: Int = products.get.size

  @PostConstruct
  def load(): IO[Unit] =
    IO.blocking(Map("tea" -> 4, "coffee" -> 6)).map(loaded => products.set(loaded.filter(_ => report.applied > 0)))

  @PreDestroy
  def clear(): IO[Unit] =
    IO(products.set(Map.empty))
}
