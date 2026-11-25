package dev.coilix.catseffect
package tagless

import cats.effect.Temporal
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import dev.coilix.annotation.Component
import scala.concurrent.duration.DurationInt

/** A service in tagless final style that waits with `Temporal` and asks the price list. */
@Component
final class QuoteService[F[_]: Temporal](prices: PriceList[F]) {

  /** A quote for `part` after a short pause. */
  def quote(part: String): F[String] =
    Temporal[F].sleep(1.milli) >> prices.price(part).map(_.fold(s"$part: unknown")(price => s"$part: $price"))
}
