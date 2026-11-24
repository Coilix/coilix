package com.example
package pekko

import dev.coilix.annotation.Service
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import scala.concurrent.Future

@Service
class OrderTotals(using Materializer) {

  def total(amounts: List[BigDecimal]): Future[BigDecimal] =
    Source(amounts).filter(_ > 0).runWith(Sink.fold(BigDecimal(0))(_ + _))
}
