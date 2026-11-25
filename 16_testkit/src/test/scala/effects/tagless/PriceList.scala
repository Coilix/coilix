package dev.coilix.testkit
package effects
package tagless

import cats.effect.Sync
import dev.coilix.annotation.Component

/** Prices in tagless final style, which needs only `Sync`. */
@Component
final class PriceList[F[_]: Sync] {
  private val prices = Map("brake pads" -> 120, "oil" -> 45)

  /** The price of `part`, if known. */
  def price(part: String): F[Option[Int]] = Sync[F].delay(prices.get(part))
}
