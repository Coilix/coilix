package com.example
package catseffect

import cats.effect.Sync
import cats.syntax.functor.*
import dev.coilix.annotation.Component

@Component
final class ShippingQuotes[F[_]: Sync](rates: ShippingRates[F]) {

  def quote(country: String, parcels: Int): F[Int] = rates.rate(country).map(_ * parcels)
}
