package com.example
package catseffect

import cats.effect.Sync
import dev.coilix.annotation.Component

@Component
final class ShippingRates[F[_]: Sync] {
  private val rates = Map("PL" -> 15, "DE" -> 25)

  def rate(country: String): F[Int] = Sync[F].delay(rates.getOrElse(country, 40))
}
