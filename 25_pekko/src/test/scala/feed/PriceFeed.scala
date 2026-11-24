package dev.coilix.pekko
package feed

import dev.coilix.annotation.Service
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import scala.concurrent.Future

@Service
final class PriceFeed(using Materializer) {
  def threadOfLatest: Future[String] = Source.single(1).map(_ => Thread.currentThread.getName).runWith(Sink.head)
}
