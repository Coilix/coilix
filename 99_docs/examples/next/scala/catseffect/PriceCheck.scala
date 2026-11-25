package com.example
package catseffect

import cats.effect.IO
import dev.coilix.annotation.Component
import dev.coilix.annotation.Order
import dev.coilix.boot.ApplicationArguments
import dev.coilix.boot.IOApplicationRunner
import java.util.concurrent.atomic.AtomicInteger

@Component
@Order(1)
class PriceCheck(cache: ProductCache) extends IOApplicationRunner {
  private val checked = AtomicInteger()

  def checkedProducts: Int = checked.get

  override def run(arguments: ApplicationArguments): IO[Unit] =
    IO(checked.set(cache.size))
}
