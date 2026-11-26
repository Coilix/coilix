package com.example
package processing

import cats.effect.IO
import dev.coilix.annotation.Repository
import fs2.Stream
import java.util.concurrent.atomic.AtomicInteger

@Repository
final class OrderFeed {
  private val read = AtomicInteger()

  val incoming: List[String] = List("order-1", "order-2", "order-3")

  def orders: Stream[IO, String] =
    Stream.emits(incoming).evalTap(_ => IO(read.incrementAndGet()).void) ++ Stream.never[IO]

  def readCount: Int = read.get
}
