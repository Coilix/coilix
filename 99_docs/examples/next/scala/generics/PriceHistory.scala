package com.example
package generics

import dev.coilix.annotation.Component

@Component
final class PriceHistory extends PriceSource[List] {
  override def price(product: String): List[BigDecimal] = List(BigDecimal(10), BigDecimal(12))
}
