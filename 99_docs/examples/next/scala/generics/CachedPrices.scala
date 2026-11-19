package com.example
package generics

import dev.coilix.annotation.Component

@Component
final class CachedPrices extends PriceSource[Option] {
  private val prices = Map("tea" -> BigDecimal(12))

  override def price(product: String): Option[BigDecimal] = prices.get(product)
}
